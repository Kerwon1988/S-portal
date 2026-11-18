package com.simbio.core.config.errortracking;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.spi.CurrentUser;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErrorInventoryUnavailableIT {

    private static final UUID ADMIN_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final Path INVENTORY_PATH = createUnreadableInventory();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUser currentUser;

    @DynamicPropertySource
    static void configureInventory(DynamicPropertyRegistry registry) {
        registry.add("portal.error-tracking.inventory-path", INVENTORY_PATH::toString);
    }

    @BeforeEach
    void provideCurrentUser() {
        when(currentUser.currentUser()).thenReturn(Optional.of(
                new AuthenticatedUser(ADMIN_ID, Role.ADMIN, Set.of(Permission.ERROR_INVENTORY_MANAGE))));
    }

    @Test
    void rendersDegradedPageWithoutInventoryDataOrPrivatePath() throws Exception {
        mockMvc.perform(get("/gestion/errores")
                        .with(user("admin").authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"),
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "ERROR_INVENTORY_MANAGE"))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.containsString("El almacenamiento no está disponible"),
                        org.hamcrest.Matchers.containsString("Storage is unavailable"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(INVENTORY_PATH.toString()))));
    }

    @Test
    void degradesSafelyWhenAnAdminTriesToUpdateWhileStorageIsUnavailable() throws Exception {
        mockMvc.perform(post("/gestion/errores/{fingerprint}/estado", "a".repeat(64))
                .with(user("admin").authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"),
                        new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ERROR_INVENTORY_MANAGE")))
                .with(csrf())
                .param("status", "RESOLVED")
                .param("resolutionNote", "Synthetic operational note"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/gestion/errores*"));

        mockMvc.perform(get("/gestion/errores")
                .with(user("admin").authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"),
                        new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ERROR_INVENTORY_MANAGE"))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.anyOf(
                org.hamcrest.Matchers.containsString("El almacenamiento no está disponible"),
                org.hamcrest.Matchers.containsString("Storage is unavailable"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString(INVENTORY_PATH.toString()))));
    }

    @AfterAll
    static void removeSyntheticInventory() throws IOException {
        Files.deleteIfExists(INVENTORY_PATH);
        Files.deleteIfExists(INVENTORY_PATH.getParent());
    }

    private static Path createUnreadableInventory() {
        try {
            Path directory = Files.createTempDirectory("sportal-error-inventory-");
            Path inventoryPath = directory.resolve("error-inventory.json");
            Files.writeString(inventoryPath, "invalid-json");
            return inventoryPath;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
