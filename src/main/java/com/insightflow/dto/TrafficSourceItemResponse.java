package com.insightflow.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrafficSourceItemResponse {
    private final String source;
    private final TrafficSourceType sourceType;
    private final long sessions;
    private final long uniqueVisitors;
    private final double percentage;
}
