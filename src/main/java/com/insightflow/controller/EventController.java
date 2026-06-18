package com.insightflow.controller;

import com.insightflow.constants.AppConstants;
import com.insightflow.dto.*;
import com.insightflow.entity.User;
import com.insightflow.service.EventService;
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
@RequestMapping("/events")
@Tag(name = "Events", description = "Event management endpoints")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @Operation(summary = "Record an event")
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal User currentUser) {
        EventResponse response = eventService.createEvent(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Event recorded", response));
    }

    @GetMapping
    @Operation(summary = "Get events for a project with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<EventResponse>>> getEvents(
            @RequestParam Integer projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIR) String sortDir,
            @AuthenticationPrincipal User currentUser) {
        PagedResponse<EventResponse> response = eventService.getEvents(
                projectId, page, size, sortBy, sortDir, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID")
    public ResponseEntity<ApiResponse<EventResponse>> getEventById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        EventResponse response = eventService.getEventById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
