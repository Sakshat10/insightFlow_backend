package com.insightflow.controller;

import com.insightflow.constants.AppConstants;
import com.insightflow.dto.*;
import com.insightflow.entity.User;
import com.insightflow.service.PageViewService;
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
@RequestMapping("/page-views")
@Tag(name = "Page Views", description = "Page view management endpoints")
public class PageViewController {

    private final PageViewService pageViewService;

    public PageViewController(PageViewService pageViewService) {
        this.pageViewService = pageViewService;
    }

    @PostMapping
    @Operation(summary = "Record a page view")
    public ResponseEntity<ApiResponse<PageViewResponse>> createPageView(
            @Valid @RequestBody CreatePageViewRequest request,
            @AuthenticationPrincipal User currentUser) {
        PageViewResponse response = pageViewService.createPageView(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Page view recorded", response));
    }

    @GetMapping
    @Operation(summary = "Get page views for a project with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<PageViewResponse>>> getPageViews(
            @RequestParam Integer projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIR) String sortDir,
            @AuthenticationPrincipal User currentUser) {
        PagedResponse<PageViewResponse> response = pageViewService.getPageViews(
                projectId, page, size, sortBy, sortDir, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get page view by ID")
    public ResponseEntity<ApiResponse<PageViewResponse>> getPageViewById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        PageViewResponse response = pageViewService.getPageViewById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
