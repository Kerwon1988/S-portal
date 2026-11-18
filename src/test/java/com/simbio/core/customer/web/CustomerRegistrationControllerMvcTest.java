package com.simbio.core.customer.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerRegistrationControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registrationFormRendersForVisitors() throws Exception {
        mockMvc.perform(get("/registro").header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/registration"));
    }

    @Test
    void registrationMutationRequiresCsrf() throws Exception {
        mockMvc.perform(post("/registro"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerInvitationRequiresAdminRoleAndCsrf() throws Exception {
        mockMvc.perform(get("/gestion/clientes/nuevo")
                        .with(user("employee").authorities(
                                new SimpleGrantedAuthority("ROLE_EMPLOYEE"),
                                new SimpleGrantedAuthority("USERS_MANAGE"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/gestion/clientes/nuevo")
                        .with(user("admin").authorities(
                                new SimpleGrantedAuthority("ROLE_ADMIN"),
                                new SimpleGrantedAuthority("USERS_MANAGE"))))
                .andExpect(status().isForbidden());
    }
}
