package com.insightflow.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TrafficResponse {

    private final List<TrafficDataPoint> dataPoints;
    private final String granularity;
}
