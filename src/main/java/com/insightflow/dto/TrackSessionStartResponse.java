package com.insightflow.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrackSessionStartResponse {

    private final String sessionId;
    private final String trackingKey;
}
