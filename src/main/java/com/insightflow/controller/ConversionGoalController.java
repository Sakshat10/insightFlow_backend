package com.insightflow.controller;

import com.insightflow.dto.*;
import com.insightflow.entity.User;
import com.insightflow.service.ConversionGoalService;
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
@RequestMapping("/conversion-goals")
@Tag(name = "Conversion Goals", description = "Conversion Goal management endpoints")
public class ConversionGoalController {

    private final ConversionGoalService conversionGoalService;

    public ConversionGoalController(ConversionGoalService conversionGoalService) {
        this.conversionGoalService = conversionGoalService;
    }

    @PostMapping
    @Operation(summary = "Create a new conversion goal or reactivate an inactive one")
    public ResponseEntity<ApiResponse<ConversionGoalResponse>> createConversionGoal(
            @Valid @RequestBody CreateConversionGoalRequest request,
            @AuthenticationPrincipal User currentUser) {
        ConversionGoalResponse response = conversionGoalService.createConversionGoal(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Conversion goal created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all conversion goals for a project")
    public ResponseEntity<ApiResponse<List<ConversionGoalResponse>>> getConversionGoals(
            @RequestParam Integer projectId,
            @AuthenticationPrincipal User currentUser) {
        List<ConversionGoalResponse> response = conversionGoalService.getConversionGoalsByProject(projectId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get conversion goal by ID")
    public ResponseEntity<ApiResponse<ConversionGoalResponse>> getConversionGoalById(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {
        ConversionGoalResponse response = conversionGoalService.getConversionGoalById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update an existing conversion goal name or status")
    public ResponseEntity<ApiResponse<ConversionGoalResponse>> updateConversionGoal(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateConversionGoalRequest request,
            @AuthenticationPrincipal User currentUser) {
        ConversionGoalResponse response = conversionGoalService.updateConversionGoal(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Conversion goal updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete / deactivate a conversion goal")
    public ResponseEntity<ApiResponse<ConversionGoalResponse>> deactivateConversionGoal(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {
        ConversionGoalResponse response = conversionGoalService.deactivateConversionGoal(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Conversion goal deactivated successfully", response));
    }

    @PostMapping("/reconcile")
    @Operation(summary = "Reconcile conversion status of historical events for a project")
    public ResponseEntity<ApiResponse<Void>> reconcile(
            @RequestParam Integer projectId,
            @AuthenticationPrincipal User currentUser) {
        conversionGoalService.reconcileProjectConversions(projectId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Reconciliation completed successfully"));
    }
}
