package com.backend.old_bicycle_project.controller;

import com.backend.old_bicycle_project.dto.assistant.AssistantChatRequestDTO;
import com.backend.old_bicycle_project.dto.assistant.AssistantChatResponseDTO;
import com.backend.old_bicycle_project.dto.response.ApiResponse;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.service.AssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping("/chat")
    public ApiResponse<AssistantChatResponseDTO> chat(
            @Valid @RequestBody AssistantChatRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ApiResponse.<AssistantChatResponseDTO>builder()
                .result(assistantService.chat(request, currentUser))
                .build();
    }
}
