package com.insightflow.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrafficDataPoint {

    private final String date;
    private final long pageViews;
    private final long sessions;
}
