package com.backend.old_bicycle_project.security;

import com.backend.old_bicycle_project.dto.response.AdminUserResponseDTO;
import com.backend.old_bicycle_project.service.InspectionService;
import com.backend.old_bicycle_project.service.PayoutService;
import com.backend.old_bicycle_project.service.RefundService;
import com.backend.old_bicycle_project.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private RefundService refundService;

    @MockBean
    private InspectionService inspectionService;

    @MockBean
    private PayoutService payoutService;

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
    void anonymousUserCannotAccessSellerOwnedProductDetail() throws Exception {
        mockMvc.perform(get("/api/products/my/11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousUserCannotConfirmOrderReceipt() throws Exception {
        mockMvc.perform(patch("/api/orders/11111111-1111-1111-1111-111111111111/confirm-received"))
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
    @WithMockUser(roles = "BUYER")
    void nonAdminUserCannotAccessAdminRefunds() throws Exception {
        mockMvc.perform(get("/api/admin/refunds"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousUserCannotAccessOwnPayoutProfile() throws Exception {
        mockMvc.perform(get("/api/payout-profiles/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void authenticatedUserCanAccessOwnPayoutProfile() throws Exception {
        mockMvc.perform(get("/api/payout-profiles/me"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void nonAdminUserCannotAccessAdminPayouts() throws Exception {
        mockMvc.perform(get("/api/admin/payouts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void buyerCannotAccessSellerOwnedProductDetail() throws Exception {
        mockMvc.perform(get("/api/products/my/11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousUserCannotAccessInspectionDashboard() throws Exception {
        mockMvc.perform(get("/api/inspections/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void buyerCannotAccessInspectionRequests() throws Exception {
        mockMvc.perform(get("/api/inspections/requests"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void sellerCannotRouteProductToInspectionQueue() throws Exception {
        mockMvc.perform(post("/api/inspections/request/11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "INSPECTOR")
    void inspectorCanAccessInspectionDashboard() throws Exception {
        when(inspectionService.getInspectionDashboard(any()))
                .thenReturn(com.backend.old_bicycle_project.dto.response.InspectionDashboardResponseDTO.builder()
                        .pendingRequests(0)
                        .completedThisWeek(0)
                        .passRate(java.math.BigDecimal.ZERO)
                        .averageScore(java.math.BigDecimal.ZERO)
                        .recentInspections(java.util.List.of())
                        .build());

        mockMvc.perform(get("/api/inspections/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void sellerCannotConfirmOrderReceipt() throws Exception {
        mockMvc.perform(patch("/api/orders/11111111-1111-1111-1111-111111111111/confirm-received"))
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

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminUserCanAccessAdminRefunds() throws Exception {
        when(refundService.getAdminRefunds(isNull(), isNull(), eq(0), eq(12)))
                .thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/api/admin/refunds"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminUserCanAccessAdminPayouts() throws Exception {
        when(payoutService.getAdminPayouts(isNull(), isNull(), isNull(), eq(0), eq(12)))
                .thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/api/admin/payouts"))
                .andExpect(status().isOk());
    }

    @Test
    void adminUserCanRouteProductToInspectionQueue() throws Exception {
        when(inspectionService.requestInspection(any(), any()))
                .thenReturn(com.backend.old_bicycle_project.dto.response.InspectionResponseDTO.builder()
                        .productId(java.util.UUID.randomUUID())
                        .build());

        com.backend.old_bicycle_project.entity.User admin = com.backend.old_bicycle_project.entity.User.builder()
                .id(java.util.UUID.randomUUID())
                .role(com.backend.old_bicycle_project.entity.enums.AppRole.admin)
                .email("admin@test.dev")
                .build();

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                admin,
                "password",
                AuthorityUtils.createAuthorityList("ROLE_ADMIN")
        );

        mockMvc.perform(
                        patch("/api/admin/products/11111111-1111-1111-1111-111111111111/send-to-inspection")
                                .with(authentication(authenticationToken))
                )
                .andExpect(status().isOk());
    }
}
