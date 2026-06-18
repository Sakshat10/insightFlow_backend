package com.insightflow.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectStatsResponse {

    private final long totalProjects;
    private final long activeProjects;
    private final long inactiveProjects;
    private final long totalPageViews;
    private final long totalSessions;
    private final long totalEvents;
}
