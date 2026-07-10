package com.insightflow.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class TrafficSourcesResponse {
    private final long totalSessions;
    private final List<TrafficSourceItemResponse> sources;
}
