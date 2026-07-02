package com.insightflow.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class EventTimelineDay {
    private final LocalDate date;
    private final List<EventTimelineItem> events;
}
