package com.insightflow.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FunnelStepResponse {
    private final int step;
    private final String eventName;
    private final long sessions;
    private final double conversionFromPrevious;
    private final double conversionFromEntry;
    private final long dropOffSessions;
    private final double dropOffRate;
}
