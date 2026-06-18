package com.insightflow.controller;

import com.insightflow.constants.AppConstants;
import com.insightflow.dto.*;
import com.insightflow.entity.User;
import com.insightflow.service.ProjectService;
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
@RequestMapping("/projects")
@Tag(name = "Projects", description = "Project management endpoints")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @Operation(summary = "Create a new project")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal User currentUser) {
        ProjectResponse response = projectService.createProject(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all projects with pagination, filtering, and sorting")
    public ResponseEntity<ApiResponse<PagedResponse<ProjectResponse>>> getAllProjects(
            @RequestParam(required = false) Integer projectStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIR) String sortDir,
            @AuthenticationPrincipal User currentUser) {
        PagedResponse<ProjectResponse> response = projectService.getAllProjects(
                projectStatus, page, size, sortBy, sortDir, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project by ID")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProjectById(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {
        ProjectResponse response = projectService.getProjectById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update project")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal User currentUser) {
        ProjectResponse response = projectService.updateProject(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Project updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete project")
    public ResponseEntity<ApiResponse<ProjectResponse>> deleteProject(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {
        ProjectResponse response = projectService.deleteProject(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Project deleted successfully", response));
    }

    @PutMapping("/{id}/restore")
    @Operation(summary = "Restore soft-deleted project")
    public ResponseEntity<ApiResponse<ProjectResponse>> restoreProject(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {
        ProjectResponse response = projectService.restoreProject(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Project restored successfully", response));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get project statistics for current user")
    public ResponseEntity<ApiResponse<ProjectStatsResponse>> getProjectStats(
            @AuthenticationPrincipal User currentUser) {
        ProjectStatsResponse response = projectService.getProjectStats(currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

