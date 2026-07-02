package com.insightflow.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventTimelineItem {
    private final String eventName;
    private final Long count;
}
