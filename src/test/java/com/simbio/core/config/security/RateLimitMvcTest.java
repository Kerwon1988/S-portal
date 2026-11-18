package com.simbio.core.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(properties = {
        "portal.security.rate-limit.ip-capacity=1",
        "portal.security.rate-limit.email-capacity=10",
        "portal.security.rate-limit.window=PT1M"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimitProperties rateLimitProperties;

    @Test
    void blocksLoginBeforeAuthenticationAndProvidesRetryAfter() throws Exception {
        assertThat(rateLimitProperties.ipCapacity()).isEqualTo(1);
        RequestPostProcessor remoteClient = request -> {
            request.setServletPath("/acceso");
            request.setRemoteAddr("198.51.100.203");
            return request;
        };
        mockMvc.perform(post("/acceso")
                        .with(remoteClient)
                        .with(csrf())
                        .header("X-Forwarded-For", "203.0.113.1")
                        .param("username", "synthetic@example.test")
                        .param("password", "synthetic-password"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/acceso")
                        .with(remoteClient)
                        .with(csrf())
                        .header("X-Forwarded-For", "203.0.113.2")
                        .param("username", "other@example.test")
                        .param("password", "synthetic-password"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }
}
