package com.insightflow.dto;

import com.insightflow.entity.LiveActivityType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class LiveActivityResponse {
    private final String activityId;
    private final LiveActivityType type;
    private final Integer projectId;
    private final Long sourceId;
    private final String visitorId;
    private final String sessionId;
    private final String eventName;
    private final String title;
    private final String url;
    private final String country;
    private final String browser;
    private final String deviceType;
    private final Boolean isConversion;
    private final LocalDateTime timestamp;
}
