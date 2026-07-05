package com.insightflow.dto;

import java.time.LocalDateTime;

public interface FunnelEventProjection {
    Long getId();
    Long getSessionId();
    String getEventName();
    LocalDateTime getCreatedAt();
}
