package com.insightflow.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OverviewAnalyticsResponse {

    private final long totalPageViews;
    private final long totalSessions;
    private final long totalEvents;
    private final long uniqueVisitors;
    private final double bounceRate;
    private final double avgSessionDurationSeconds;
    private final long pageViewsToday;
    private final long sessionsToday;
}
