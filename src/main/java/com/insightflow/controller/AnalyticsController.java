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

import java.time.LocalDate;
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

    @GetMapping("/event-timeline")
    @Operation(summary = "Get daily custom event activity timeline")
    public ResponseEntity<ApiResponse<EventTimelineResponse>> getEventTimeline(
            @RequestParam Integer projectId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getEventTimeline(projectId, from, to, currentUser)));
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
    public ResponseEntity<ApiResponse<List<EventAnalyticsResponse>>> getEventAnalytics(
            @RequestParam Integer projectId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getEventAnalytics(projectId, from, to, limit, currentUser)));
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

    @GetMapping("/funnel")
    @Operation(summary = "Get ordered session-based funnel analytics")
    public ResponseEntity<ApiResponse<FunnelAnalyticsResponse>> getFunnel(
            @RequestParam Integer projectId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam List<String> steps,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getFunnel(projectId, from, to, steps, currentUser)));
    }

    @GetMapping("/traffic-sources")
    @Operation(summary = "Get traffic sources by acquisition source")
    public ResponseEntity<ApiResponse<TrafficSourcesResponse>> getTrafficSources(
            @RequestParam Integer projectId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getTrafficSources(projectId, from, to, currentUser)));
    }

    @GetMapping("/conversions")
    @Operation(summary = "Get daily conversion statistics for a project")
    public ResponseEntity<ApiResponse<List<DailyConversionResponse>>> getDailyConversions(
            @RequestParam Integer projectId,
            @RequestParam(required = false) Integer days,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getDailyConversions(projectId, days, currentUser)));
    }
}
