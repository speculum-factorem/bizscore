package com.bizscore.security;

import com.bizscore.config.JwtAuthenticationFilter;
import com.bizscore.service.CustomUserDetailsService;
import com.bizscore.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тесты безопасности
 * Проверяет доступ к защищенным endpoint'ам
 */
@WebMvcTest
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthFilter;

    @Test
    @WithAnonymousUser
    void accessPublicEndpoint_ShouldSucceed() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void accessProtectedEndpoint_WithoutAuth_ShouldFail() throws Exception {
        mockMvc.perform(get("/api/scores")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void accessProtectedEndpoint_WithAuth_ShouldSucceed() throws Exception {
        mockMvc.perform(get("/api/scores")))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void accessAdminEndpoint_WithUserRole_ShouldFail() throws Exception {
        mockMvc.perform(get("/api/v2/scoring/batch")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void accessAdminEndpoint_WithAdminRole_ShouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v2/scoring/batch")))
                .andExpect(status().isOk());
    }
}