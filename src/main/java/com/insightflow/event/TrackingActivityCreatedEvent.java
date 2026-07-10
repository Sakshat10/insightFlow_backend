package com.insightflow.event;

import com.insightflow.entity.LiveActivityType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

@Getter
public class TrackingActivityCreatedEvent extends ApplicationEvent {
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
    private final LocalDateTime activityTimestamp;

    public TrackingActivityCreatedEvent(
            Object source,
            String activityId,
            LiveActivityType type,
            Integer projectId,
            Long sourceId,
            String visitorId,
            String sessionId,
            String eventName,
            String title,
            String url,
            String country,
            String browser,
            String deviceType,
            Boolean isConversion,
            LocalDateTime activityTimestamp) {
        super(source);
        this.activityId = activityId;
        this.type = type;
        this.projectId = projectId;
        this.sourceId = sourceId;
        this.visitorId = visitorId;
        this.sessionId = sessionId;
        this.eventName = eventName;
        this.title = title;
        this.url = url;
        this.country = country;
        this.browser = browser;
        this.deviceType = deviceType;
        this.isConversion = isConversion;
        this.activityTimestamp = activityTimestamp;
    }
}
