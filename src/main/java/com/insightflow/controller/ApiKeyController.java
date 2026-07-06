package com.insightflow.controller;

import com.insightflow.dto.*;
import com.insightflow.entity.User;
import com.insightflow.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api-keys")
@Tag(name = "API Keys", description = "API Key management endpoints")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    @Operation(summary = "Create a new API key")
    public ResponseEntity<ApiResponse<ApiKeyCreatedResponse>> createApiKey(
            @Valid @RequestBody CreateApiKeyRequest request,
            @AuthenticationPrincipal User currentUser) {
        ApiKeyCreatedResponse response = apiKeyService.createApiKey(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("API Key created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all API keys for a project")
    public ResponseEntity<ApiResponse<List<ApiKeyResponse>>> getApiKeys(
            @RequestParam Integer projectId,
            @AuthenticationPrincipal User currentUser) {
        List<ApiKeyResponse> response = apiKeyService.getApiKeysByProject(projectId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get API key by ID")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> getApiKeyById(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {
        ApiKeyResponse response = apiKeyService.getApiKeyById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/revoke")
    @Operation(summary = "Revoke an API key")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> revokeApiKey(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {
        ApiKeyResponse response = apiKeyService.revokeApiKey(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("API Key revoked successfully", response));
    }

    @PostMapping("/{id}/rotate")
    @Operation(summary = "Rotate an API key")
    public ResponseEntity<ApiResponse<ApiKeyCreatedResponse>> rotateApiKey(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {
        ApiKeyCreatedResponse response = apiKeyService.rotateApiKey(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("API Key rotated successfully", response));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get API key statistics for a project")
    public ResponseEntity<ApiResponse<ApiKeyStatsResponse>> getApiKeyStats(
            @RequestParam Integer projectId,
            @AuthenticationPrincipal User currentUser) {
        ApiKeyStatsResponse response = apiKeyService.getApiKeyStats(projectId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
