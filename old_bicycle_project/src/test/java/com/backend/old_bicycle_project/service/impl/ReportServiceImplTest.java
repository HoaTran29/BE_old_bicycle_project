package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.config.NotificationEvent;
import com.backend.old_bicycle_project.dto.request.ReportProcessDTO;
import com.backend.old_bicycle_project.dto.request.ReportRequestDTO;
import com.backend.old_bicycle_project.dto.response.ReportResponseDTO;
import com.backend.old_bicycle_project.entity.Product;
import com.backend.old_bicycle_project.entity.Report;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.ProductStatus;
import com.backend.old_bicycle_project.entity.enums.ReportReason;
import com.backend.old_bicycle_project.entity.enums.ReportStatus;
import com.backend.old_bicycle_project.entity.enums.UserStatus;
import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.repository.ProductRepository;
import com.backend.old_bicycle_project.repository.ReportRepository;
import com.backend.old_bicycle_project.repository.UserRepository;
import com.backend.old_bicycle_project.service.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    void submitReportNotifiesAdminsAboutPendingReview() {
        UUID reporterId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        User reporter = user(reporterId, AppRole.buyer);
        User admin = user(adminId, AppRole.admin);

        when(userRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(userRepository.findByRole(AppRole.admin)).thenReturn(List.of(admin));
        when(productRepository.existsById(targetId)).thenReturn(true);
        when(reportRepository.existsByReporterIdAndTargetIdAndStatusIn(any(), any(), any())).thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            if (report.getId() == null) {
                report.setId(UUID.randomUUID());
            }
            return report;
        });

        ReportResponseDTO response = reportService.submitReport(reporterId, ReportRequestDTO.builder()
                .targetId(targetId)
                .targetType("PRODUCT")
                .reason(ReportReason.spam)
                .description("Listing contains suspicious content")
                .build(), null);

        assertThat(response.getStatus()).isEqualTo(ReportStatus.pending);
        verify(eventPublisher).publishEvent(any(NotificationEvent.class));
    }

    @Test
    void submitReportStoresEvidenceFilesWhenImagesAreProvided() {
        UUID reporterId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User reporter = user(reporterId, AppRole.buyer);

        when(userRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(userRepository.findByRole(AppRole.admin)).thenReturn(List.of());
        when(productRepository.existsById(targetId)).thenReturn(true);
        when(reportRepository.existsByReporterIdAndTargetIdAndStatusIn(any(), any(), any())).thenReturn(false);
        when(storageService.uploadFile(any(), any())).thenReturn("https://cdn.example.com/report-1.jpg");
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            if (report.getId() == null) {
                report.setId(UUID.randomUUID());
            }
            report.getEvidenceFiles().forEach(file -> {
                if (file.getId() == null) {
                    file.setId(UUID.randomUUID());
                }
            });
            return report;
        });

        MockMultipartFile image = new MockMultipartFile(
                "files",
                "listing-proof.jpg",
                "image/jpeg",
                "fake-image".getBytes()
        );

        ReportResponseDTO response = reportService.submitReport(reporterId, ReportRequestDTO.builder()
                .targetId(targetId)
                .targetType("PRODUCT")
                .reason(ReportReason.fake)
                .description("Ảnh chụp không đúng xe")
                .build(), List.of(image));

        assertThat(response.getEvidenceFiles()).hasSize(1);
        assertThat(response.getEvidenceFiles().get(0).getFileName()).isEqualTo("listing-proof.jpg");
        verify(storageService).uploadFile(any(), any());
    }

    @Test
    void submitReportRejectsDuplicateOpenReportForSameReporterAndTarget() {
        UUID reporterId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User reporter = user(reporterId, AppRole.buyer);

        when(userRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(productRepository.existsById(targetId)).thenReturn(true);
        when(reportRepository.existsByReporterIdAndTargetIdAndStatusIn(any(), any(), any())).thenReturn(true);

        ReportRequestDTO request = ReportRequestDTO.builder()
                .targetId(targetId)
                .targetType("PRODUCT")
                .reason(ReportReason.spam)
                .description("Spam listing")
                .build();

        assertThatThrownBy(() -> reportService.submitReport(reporterId, request, null))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RECORD_ALREADY_EXISTS));
    }

    @Test
    void processReportCanMovePendingReportIntoInvestigating() {
        UUID reportId = UUID.randomUUID();
        UUID reporterId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        User reporter = user(reporterId, AppRole.buyer);
        User admin = user(adminId, AppRole.admin);
        Report report = Report.builder()
                .id(reportId)
                .reporter(reporter)
                .targetId(UUID.randomUUID())
                .targetType("PRODUCT")
                .reason(ReportReason.spam)
                .status(ReportStatus.pending)
                .build();

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportResponseDTO response = reportService.processReport(
                reportId,
                ReportProcessDTO.builder()
                        .status(ReportStatus.investigating)
                        .adminNote("Cần thêm bằng chứng")
                        .build(),
                adminId
        );

        assertThat(response.getStatus()).isEqualTo(ReportStatus.investigating);
        assertThat(response.getAdminNote()).isEqualTo("Cần thêm bằng chứng");
        assertThat(response.getProcessedById()).isEqualTo(adminId);
        assertThat(response.getProcessedAt()).isNotNull();
        verify(productRepository, never()).save(any(Product.class));
        verify(userRepository, never()).save(any(User.class));
        verify(eventPublisher, times(1)).publishEvent(any());
    }

    @Test
    void processReportAddsAuditFieldsAndAppliesUserSanctionWhenViolationIsUpheld() {
        UUID reportId = UUID.randomUUID();
        UUID reporterId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        User reporter = user(reporterId, AppRole.buyer);
        User targetUser = user(targetUserId, AppRole.seller);
        User admin = user(adminId, AppRole.admin);
        Report report = Report.builder()
                .id(reportId)
                .reporter(reporter)
                .targetId(targetUserId)
                .targetType("USER")
                .reason(ReportReason.fraud)
                .status(ReportStatus.investigating)
                .build();

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportResponseDTO response = reportService.processReport(
                reportId,
                ReportProcessDTO.builder()
                        .status(ReportStatus.resolved_upheld)
                        .adminNote("Đã xác minh và khóa tài khoản")
                        .build(),
                adminId
        );

        assertThat(response.getStatus()).isEqualTo(ReportStatus.resolved_upheld);
        assertThat(response.getAdminNote()).isEqualTo("Đã xác minh và khóa tài khoản");
        assertThat(response.getProcessedById()).isEqualTo(adminId);
        assertThat(response.getProcessedAt()).isNotNull();
        assertThat(targetUser.getStatus()).isEqualTo(UserStatus.banned);
        verify(eventPublisher, times(2)).publishEvent(any());
    }

    @Test
    void processReportDismissesCaseWithoutApplyingSanction() {
        UUID reportId = UUID.randomUUID();
        UUID reporterId = UUID.randomUUID();
        UUID targetProductId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        User reporter = user(reporterId, AppRole.buyer);
        User admin = user(adminId, AppRole.admin);
        Product product = Product.builder()
                .id(targetProductId)
                .status(ProductStatus.active)
                .build();
        Report report = Report.builder()
                .id(reportId)
                .reporter(reporter)
                .targetId(targetProductId)
                .targetType("PRODUCT")
                .reason(ReportReason.other)
                .status(ReportStatus.investigating)
                .build();

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportResponseDTO response = reportService.processReport(
                reportId,
                ReportProcessDTO.builder()
                        .status(ReportStatus.resolved_dismissed)
                        .adminNote("Chưa đủ bằng chứng để kết luận vi phạm")
                        .build(),
                adminId
        );

        assertThat(response.getStatus()).isEqualTo(ReportStatus.resolved_dismissed);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.active);
        verify(productRepository, never()).save(any(Product.class));
        verify(userRepository, never()).save(any(User.class));
        verify(eventPublisher, times(1)).publishEvent(any());
    }

    @Test
    void processReportRejectsInvalidTransitionFromClosedState() {
        UUID reportId = UUID.randomUUID();
        UUID reporterId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        User reporter = user(reporterId, AppRole.buyer);
        User admin = user(adminId, AppRole.admin);
        Report report = Report.builder()
                .id(reportId)
                .reporter(reporter)
                .targetId(UUID.randomUUID())
                .targetType("PRODUCT")
                .reason(ReportReason.fake)
                .status(ReportStatus.resolved_upheld)
                .build();

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> reportService.processReport(
                reportId,
                ReportProcessDTO.builder()
                        .status(ReportStatus.resolved_dismissed)
                        .adminNote("Không hợp lệ")
                        .build(),
                adminId
        )).isInstanceOfSatisfying(AppException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REPORT_STATUS_TRANSITION));

        verify(reportRepository, never()).save(any(Report.class));
    }

    private User user(UUID userId, AppRole role) {
        return User.builder()
                .id(userId)
                .email(role.name().toLowerCase() + "@test.dev")
                .firstName("Test")
                .lastName(role.name())
                .role(role)
                .status(UserStatus.active)
                .build();
    }
}
