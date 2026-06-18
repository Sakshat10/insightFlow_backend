package com.insightflow.controller;

import com.insightflow.dto.*;
import com.insightflow.entity.User;
import com.insightflow.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/analytics")
@Tag(name = "Analytics", description = "Analytics and reporting endpoints")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Get overview analytics for a project")
    public ResponseEntity<ApiResponse<OverviewAnalyticsResponse>> getOverview(
            @RequestParam Integer projectId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getOverview(projectId, currentUser)));
    }

    @GetMapping("/traffic")
    @Operation(summary = "Get daily traffic data for a project")
    public ResponseEntity<ApiResponse<TrafficResponse>> getTraffic(
            @RequestParam Integer projectId,
            @RequestParam(defaultValue = "30") int days,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getTraffic(projectId, days, currentUser)));
    }

    @GetMapping("/top-pages")
    @Operation(summary = "Get top pages by page views")
    public ResponseEntity<ApiResponse<List<StatEntry>>> getTopPages(
            @RequestParam Integer projectId,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getTopPages(projectId, limit, currentUser)));
    }

    @GetMapping("/events")
    @Operation(summary = "Get event analytics by event name")
    public ResponseEntity<ApiResponse<List<StatEntry>>> getEventAnalytics(
            @RequestParam Integer projectId,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getEventAnalytics(projectId, limit, currentUser)));
    }

    @GetMapping("/sessions")
    @Operation(summary = "Get session analytics (bounce vs engaged)")
    public ResponseEntity<ApiResponse<List<StatEntry>>> getSessionAnalytics(
            @RequestParam Integer projectId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getSessionAnalytics(projectId, currentUser)));
    }

    @GetMapping("/devices")
    @Operation(summary = "Get device type distribution")
    public ResponseEntity<ApiResponse<List<StatEntry>>> getDeviceStats(
            @RequestParam Integer projectId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getDeviceStats(projectId, currentUser)));
    }

    @GetMapping("/browsers")
    @Operation(summary = "Get browser distribution")
    public ResponseEntity<ApiResponse<List<StatEntry>>> getBrowserStats(
            @RequestParam Integer projectId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getBrowserStats(projectId, currentUser)));
    }

    @GetMapping("/countries")
    @Operation(summary = "Get country distribution")
    public ResponseEntity<ApiResponse<List<StatEntry>>> getCountryStats(
            @RequestParam Integer projectId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getCountryStats(projectId, currentUser)));
    }

    @GetMapping("/referrers")
    @Operation(summary = "Get referrer distribution")
    public ResponseEntity<ApiResponse<List<StatEntry>>> getReferrerStats(
            @RequestParam Integer projectId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getReferrerStats(projectId, currentUser)));
    }
}
