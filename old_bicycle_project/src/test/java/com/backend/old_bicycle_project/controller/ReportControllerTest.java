package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.request.ReportRequestDTO;
import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.dto.response.ReportResponseDTO;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.ReportReason;
import com.backend.old_bicycle_project.entity.enums.ReportStatus;
import com.backend.old_bicycle_project.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    @Test
    void submitReportReturnsWrappedReportAndForwardsFiles() {
        UUID reporterId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User currentUser = User.builder().id(reporterId).build();
        ReportRequestDTO requestDTO = ReportRequestDTO.builder()
                .targetId(targetId)
                .targetType("PRODUCT")
                .reason(ReportReason.fake)
                .description("Listing uses wrong images")
                .build();
        MockMultipartFile evidence = new MockMultipartFile(
                "files",
                "listing-proof.jpg",
                "image/jpeg",
                "fake-image".getBytes()
        );
        List<MockMultipartFile> files = List.of(evidence);
        ReportResponseDTO report = ReportResponseDTO.builder()
                .id(UUID.randomUUID())
                .reporterId(reporterId)
                .targetId(targetId)
                .status(ReportStatus.pending)
                .build();

        when(reportService.submitReport(reporterId, requestDTO, List.copyOf(files))).thenReturn(report);

        ApiResponse<ReportResponseDTO> response = reportController.submitReport(currentUser, requestDTO, List.copyOf(files)).getBody();

        assertThat(response).isNotNull();
        assertThat(response.getResult()).isSameAs(report);
        verify(reportService).submitReport(reporterId, requestDTO, List.copyOf(files));
    }

    @Test
    void submitReportEndpointConsumesMultipartFormData() throws NoSuchMethodException {
        PostMapping annotation = ReportController.class
                .getMethod("submitReport", User.class, ReportRequestDTO.class, List.class)
                .getAnnotation(PostMapping.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.consumes()).contains(MediaType.MULTIPART_FORM_DATA_VALUE);
    }
}
