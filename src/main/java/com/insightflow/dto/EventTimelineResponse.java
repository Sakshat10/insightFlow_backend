package com.insightflow.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class EventTimelineResponse {
    private final List<EventTimelineDay> timeline;
}
