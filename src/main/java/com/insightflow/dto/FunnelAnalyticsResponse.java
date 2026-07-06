package com.insightflow.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class FunnelAnalyticsResponse {
    private final long totalEnteredSessions;
    private final long totalConvertedSessions;
    private final double overallConversionRate;
    private final Integer biggestDropOffStep;
    private final List<FunnelStepAnalyticsResponse> steps;
}
