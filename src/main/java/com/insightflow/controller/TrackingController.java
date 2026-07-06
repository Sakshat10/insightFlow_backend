package com.insightflow.controller;

import com.insightflow.dto.*;
import com.insightflow.service.TrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/tracking")
@Tag(name = "Tracking", description = "Public tracking endpoints used by the embedded JS snippet")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping(value = "/script", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Get the embeddable JavaScript tracking snippet for a project")
    public ResponseEntity<String> getTrackingScript(
            @RequestParam("key") String apiKey,
            HttpServletRequest request) {
        String script = trackingService.getTrackingScript(apiKey, request);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/javascript"))
                .body(script);
    }

    @PostMapping("/session-start")
    @Operation(summary = "Record session start")
    public ResponseEntity<ApiResponse<TrackSessionStartResponse>> sessionStart(
            @Valid @RequestBody TrackSessionStartRequest request,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            HttpServletRequest httpRequest) {
        TrackSessionStartResponse response = trackingService.trackSessionStart(request, apiKey, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Session started", response));
    }

    @PostMapping("/session-end")
    @Operation(summary = "Record session end")
    public ResponseEntity<ApiResponse<Void>> sessionEnd(
            @Valid @RequestBody TrackSessionEndRequest request,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        trackingService.trackSessionEnd(request, apiKey);
        return ResponseEntity.ok(ApiResponse.success("Session ended"));
    }

    @PostMapping("/page-view")
    @Operation(summary = "Record a page view")
    public ResponseEntity<ApiResponse<PageViewResponse>> trackPageView(
            @Valid @RequestBody TrackPageViewRequest request,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            HttpServletRequest httpRequest) {
        PageViewResponse response = trackingService.trackPageView(request, apiKey, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Page view recorded", response));
    }

    @PostMapping("/event")
    @Operation(summary = "Record a custom event")
    public ResponseEntity<ApiResponse<EventResponse>> trackEvent(
            @Valid @RequestBody TrackEventRequest request,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            HttpServletRequest httpRequest) {
        EventResponse response = trackingService.trackEvent(request, apiKey, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Event recorded", response));
    }
}
