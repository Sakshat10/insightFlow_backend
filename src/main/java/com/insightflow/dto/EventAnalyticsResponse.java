package com.insightflow.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EventAnalyticsResponse {
    private final String eventName;
    private final long count;
    private final long uniqueUsers;
    private final String category;
    private final double impact;
    private final double trend;
    private final LocalDateTime lastSeen;
}
