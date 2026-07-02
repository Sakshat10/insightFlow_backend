package com.insightflow.dto;

import java.time.LocalDate;

public interface EventTimelineProjection {
    LocalDate getDate();
    String getEventName();
    Long getCount();
}
