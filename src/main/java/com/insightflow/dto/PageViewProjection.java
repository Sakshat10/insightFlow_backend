package com.insightflow.dto;

import java.time.LocalDateTime;

public interface PageViewProjection {

    Long getId();

    Integer getProjectId();

    Long getSessionId();

    String getUrl();

    String getTitle();

    String getReferrer();

    LocalDateTime getCreatedAt();
}