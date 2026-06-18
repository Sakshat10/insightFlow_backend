package com.insightflow.controller;

import com.insightflow.constants.AppConstants;
import com.insightflow.dto.*;
import com.insightflow.entity.User;
import com.insightflow.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/sessions")
@Tag(name = "Sessions", description = "Session management endpoints")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    @Operation(summary = "Create a new session")
    public ResponseEntity<ApiResponse<SessionResponse>> createSession(
            @Valid @RequestBody CreateSessionRequest request,
            @AuthenticationPrincipal User currentUser) {
        SessionResponse response = sessionService.createSession(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Session created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get sessions for a project with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<SessionResponse>>> getSessions(
            @RequestParam Integer projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIR) String sortDir,
            @AuthenticationPrincipal User currentUser) {
        PagedResponse<SessionResponse> response = sessionService.getSessions(
                projectId, page, size, sortBy, sortDir, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get session by ID")
    public ResponseEntity<ApiResponse<SessionResponse>> getSessionById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        SessionResponse response = sessionService.getSessionById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
