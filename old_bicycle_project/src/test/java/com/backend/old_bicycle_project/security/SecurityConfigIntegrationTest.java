package com.backend.old_bicycle_project.security;

import com.backend.old_bicycle_project.dto.response.AdminUserResponseDTO;
import com.backend.old_bicycle_project.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService adminUserService;

    @Test
    void anonymousUserCannotUpdateProfile() throws Exception {
        mockMvc.perform(patch("/api/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Anon\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousUserCannotChangePassword() throws Exception {
        mockMvc.perform(patch("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"OldPass1\",\"newPassword\":\"StrongPass1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousUserCannotAccessAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousUserCannotHideSellerProduct() throws Exception {
        mockMvc.perform(patch("/api/products/11111111-1111-1111-1111-111111111111/hide"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void nonAdminUserCannotAccessAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void nonAdminUserCannotAccessAdminProducts() throws Exception {
        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminUserCanAccessAdminUsers() throws Exception {
        when(adminUserService.getAllUsers(isNull(), isNull(), isNull(), isNull(), eq(0), eq(12)))
                .thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk());
    }
}
