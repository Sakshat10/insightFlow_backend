package com.insightflow.dto;

import java.time.LocalDateTime;

public interface EventDetailsProjection {
    Long getId();
    Long getSessionId();
    String getEventName();
    String getEventCategory();
    String getEventLabel();
    String getEventValue();
    String getUrl();
    String getProperties();
    Boolean getIsConversion();
    String getCountry();
    String getBrowser();
    String getDeviceType();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
}
