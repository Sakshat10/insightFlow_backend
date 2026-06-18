package com.insightflow.dto;

import com.insightflow.entity.Event;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EventResponse {

    private final Long id;
    private final Integer projectId;
    private final String trackingKey;
    private final String sessionId;
    private final String eventName;
    private final String eventCategory;
    private final String eventLabel;
    private final String eventValue;
    private final String url;
    private final String country;
    private final String deviceType;
    private final String browser;
    private final String properties;
    private final LocalDateTime createdAt;

    public static EventResponse from(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .projectId(event.getProjectId())
                .trackingKey(event.getTrackingKey())
                .sessionId(event.getSessionId())
                .eventName(event.getEventName())
                .eventCategory(event.getEventCategory())
                .eventLabel(event.getEventLabel())
                .eventValue(event.getEventValue())
                .url(event.getUrl())
                .country(event.getCountry())
                .deviceType(event.getDeviceType())
                .browser(event.getBrowser())
                .properties(event.getProperties())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
