package com.insightflow.controller;

import com.insightflow.dto.*;
import com.insightflow.entity.User;
import com.insightflow.service.FunnelService;
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
@RequestMapping("/funnels")
@Tag(name = "Funnels", description = "Funnel definition management endpoints")
public class FunnelController {

    private final FunnelService funnelService;

    public FunnelController(FunnelService funnelService) {
        this.funnelService = funnelService;
    }

    @PostMapping
    @Operation(summary = "Create a new funnel definition")
    public ResponseEntity<ApiResponse<FunnelResponse>> createFunnel(
            @Valid @RequestBody CreateFunnelRequest request,
            @AuthenticationPrincipal User currentUser) {
        FunnelResponse response = funnelService.createFunnel(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Funnel created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all funnels for a project")
    public ResponseEntity<ApiResponse<List<FunnelResponse>>> getFunnels(
            @RequestParam Integer projectId,
            @AuthenticationPrincipal User currentUser) {
        List<FunnelResponse> response = funnelService.getFunnelsByProject(projectId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get funnel by ID")
    public ResponseEntity<ApiResponse<FunnelResponse>> getFunnelById(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {
        FunnelResponse response = funnelService.getFunnelById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a funnel definition")
    public ResponseEntity<ApiResponse<FunnelResponse>> updateFunnel(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateFunnelRequest request,
            @AuthenticationPrincipal User currentUser) {
        FunnelResponse response = funnelService.updateFunnel(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Funnel updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a funnel definition")
    public ResponseEntity<ApiResponse<FunnelResponse>> deleteFunnel(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {
        FunnelResponse response = funnelService.deleteFunnel(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Funnel deleted successfully", response));
    }
}
