package com.insightflow.controller;

import com.insightflow.dto.ApiResponse;
import com.insightflow.dto.LiveActivityResponse;
import com.insightflow.entity.Project;
import com.insightflow.entity.User;
import com.insightflow.exception.ForbiddenException;
import com.insightflow.exception.ResourceNotFoundException;
import com.insightflow.repository.ProjectRepository;
import com.insightflow.service.LiveActivityService;
import com.insightflow.service.LiveActivityStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/live-activity")
@Tag(name = "Live Activity", description = "Live Activity and SSE stream endpoints")
public class LiveActivityController {

    private final LiveActivityService liveActivityService;
    private final LiveActivityStreamService liveActivityStreamService;
    private final ProjectRepository projectRepository;

    public LiveActivityController(LiveActivityService liveActivityService,
                                  LiveActivityStreamService liveActivityStreamService,
                                  ProjectRepository projectRepository) {
        this.liveActivityService = liveActivityService;
        this.liveActivityStreamService = liveActivityStreamService;
        this.projectRepository = projectRepository;
    }

    @GetMapping
    @Operation(summary = "Get recent live activity for a project")
    public ResponseEntity<ApiResponse<List<LiveActivityResponse>>> getRecentActivity(
            @RequestParam Integer projectId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @AuthenticationPrincipal User currentUser) {
        List<LiveActivityResponse> response = liveActivityService.getRecentActivity(projectId, limit, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to project live activity stream")
    public SseEmitter subscribe(
            @RequestParam Integer projectId,
            @AuthenticationPrincipal User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        if (!project.getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not have permission to access this project");
        }

        return liveActivityStreamService.subscribe(projectId);
    }
}
