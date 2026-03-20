package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.dto.response.OrderEvidenceSubmissionResponseDTO;
import com.backend.old_bicycle_project.entity.Order;
import com.backend.old_bicycle_project.entity.OrderEvidenceSubmission;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.OrderEvidenceType;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.repository.OrderEvidenceSubmissionRepository;
import com.backend.old_bicycle_project.service.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEvidenceServiceImplTest {

    @Mock
    private OrderEvidenceSubmissionRepository orderEvidenceSubmissionRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private OrderEvidenceServiceImpl orderEvidenceService;

    @Test
    void createSellerHandoverEvidenceRequiresAtLeastOneImage() {
        Order order = order();
        User seller = user(AppRole.seller);

        assertThatThrownBy(() -> orderEvidenceService.createSellerHandoverEvidence(order, seller, "Đã giao", List.of()))
                .isInstanceOf(AppException.class);
    }

    @Test
    void createBuyerReceiptEvidenceSkipsEmptySubmission() {
        OrderEvidenceSubmissionResponseDTO response = orderEvidenceService.createBuyerReceiptEvidence(
                order(),
                user(AppRole.buyer),
                "   ",
                List.of()
        );

        assertThat(response).isNull();
    }

    @Test
    void createSellerHandoverEvidenceUploadsImagesAndPersistsSubmission() {
        Order order = order();
        User seller = user(AppRole.seller);
        MockMultipartFile firstFile = new MockMultipartFile("files", "handover-1.jpg", "image/jpeg", new byte[]{1});
        MockMultipartFile secondFile = new MockMultipartFile("files", "handover-2.jpg", "image/jpeg", new byte[]{2});

        when(orderEvidenceSubmissionRepository.existsByOrderIdAndEvidenceType(order.getId(), OrderEvidenceType.seller_handover))
                .thenReturn(false);
        when(storageService.uploadFile(eq(firstFile), any(String.class))).thenReturn("https://cdn.test/orders/1.jpg");
        when(storageService.uploadFile(eq(secondFile), any(String.class))).thenReturn("https://cdn.test/orders/2.jpg");
        when(orderEvidenceSubmissionRepository.save(any(OrderEvidenceSubmission.class))).thenAnswer(invocation -> {
            OrderEvidenceSubmission submission = invocation.getArgument(0);
            submission.setId(UUID.randomUUID());
            submission.getFiles().get(0).setId(UUID.randomUUID());
            submission.getFiles().get(1).setId(UUID.randomUUID());
            return submission;
        });

        OrderEvidenceSubmissionResponseDTO response = orderEvidenceService.createSellerHandoverEvidence(
                order,
                seller,
                "Đã bàn giao tại cửa hàng",
                List.of(firstFile, secondFile)
        );

        ArgumentCaptor<OrderEvidenceSubmission> submissionCaptor = ArgumentCaptor.forClass(OrderEvidenceSubmission.class);
        verify(orderEvidenceSubmissionRepository).save(submissionCaptor.capture());
        OrderEvidenceSubmission savedSubmission = submissionCaptor.getValue();

        assertThat(savedSubmission.getEvidenceType()).isEqualTo(OrderEvidenceType.seller_handover);
        assertThat(savedSubmission.getSubmittedByRole()).isEqualTo(AppRole.seller);
        assertThat(savedSubmission.getFiles()).hasSize(2);
        assertThat(response.getFiles()).hasSize(2);
        assertThat(response.getNote()).isEqualTo("Đã bàn giao tại cửa hàng");
    }

    @Test
    void getEvidenceByOrderIdsGroupsSellerAndBuyerEvidence() {
        Order order = order();
        User seller = user(AppRole.seller);
        User buyer = user(AppRole.buyer);

        OrderEvidenceSubmission sellerSubmission = OrderEvidenceSubmission.builder()
                .id(UUID.randomUUID())
                .order(order)
                .submittedByUser(seller)
                .submittedByRole(AppRole.seller)
                .evidenceType(OrderEvidenceType.seller_handover)
                .build();
        OrderEvidenceSubmission buyerSubmission = OrderEvidenceSubmission.builder()
                .id(UUID.randomUUID())
                .order(order)
                .submittedByUser(buyer)
                .submittedByRole(AppRole.buyer)
                .evidenceType(OrderEvidenceType.buyer_receipt)
                .build();

        when(orderEvidenceSubmissionRepository.findDetailedByOrderIds(anyCollection()))
                .thenReturn(List.of(sellerSubmission, buyerSubmission));

        Map<UUID, Map<OrderEvidenceType, OrderEvidenceSubmissionResponseDTO>> result =
                orderEvidenceService.getEvidenceByOrderIds(List.of(order.getId()));

        assertThat(result.get(order.getId())).containsKeys(
                OrderEvidenceType.seller_handover,
                OrderEvidenceType.buyer_receipt
        );
    }

    private Order order() {
        return Order.builder()
                .id(UUID.randomUUID())
                .product(Product.builder().id(UUID.randomUUID()).title("Trek Domane").build())
                .build();
    }

    private User user(AppRole role) {
        return User.builder()
                .id(UUID.randomUUID())
                .firstName("Test")
                .lastName(role.name())
                .role(role)
                .build();
    }
}
