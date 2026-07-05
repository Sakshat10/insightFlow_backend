package com.insightflow.dto;

import com.insightflow.entity.Event;
import com.insightflow.entity.Session;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EventResponse {

    private final Long id;
    private final Long sessionId;
    private final String eventName;
    private final String eventCategory;
    private final String eventLabel;
    private final String eventValue;
    private final String url;
    private final String properties;
    private final Boolean isConversion;
    private final String country;
    private final String browser;
    private final String deviceType;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static EventResponse from(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .sessionId(event.getSessionId())
                .eventName(event.getEventName())
                .eventCategory(event.getEventCategory())
                .eventLabel(event.getEventLabel())
                .eventValue(event.getEventValue())
                .url(event.getUrl())
                .properties(event.getProperties())
                .isConversion(event.getIsConversion())
                .country(null)
                .browser(null)
                .deviceType(null)
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }

    public static EventResponse from(Event event, Session session) {
        return EventResponse.builder()
                .id(event.getId())
                .sessionId(event.getSessionId())
                .eventName(event.getEventName())
                .eventCategory(event.getEventCategory())
                .eventLabel(event.getEventLabel())
                .eventValue(event.getEventValue())
                .url(event.getUrl())
                .properties(event.getProperties())
                .isConversion(event.getIsConversion())
                .country(session != null ? session.getCountry() : null)
                .browser(session != null ? session.getBrowser() : null)
                .deviceType(session != null ? session.getDeviceType() : null)
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }

    public static EventResponse from(EventDetailsProjection projection) {
        if (projection == null) {
            return null;
        }
        return EventResponse.builder()
                .id(projection.getId())
                .sessionId(projection.getSessionId())
                .eventName(projection.getEventName())
                .eventCategory(projection.getEventCategory())
                .eventLabel(projection.getEventLabel())
                .eventValue(projection.getEventValue())
                .url(projection.getUrl())
                .properties(projection.getProperties())
                .isConversion(projection.getIsConversion())
                .country(projection.getCountry())
                .browser(projection.getBrowser())
                .deviceType(projection.getDeviceType())
                .createdAt(projection.getCreatedAt())
                .updatedAt(projection.getUpdatedAt())
                .build();
    }
}
