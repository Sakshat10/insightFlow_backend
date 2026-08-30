package com.insightflow.controller;

import com.insightflow.dto.HealthCheckResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "Health check endpoint")
public class HealthController {

    @GetMapping
    @Operation(summary = "Get application health status")
    public ResponseEntity<HealthCheckResponse> getHealth() {
        HealthCheckResponse response = new HealthCheckResponse("ok", Instant.now().toString());
        return ResponseEntity.ok(response);
    }
}
