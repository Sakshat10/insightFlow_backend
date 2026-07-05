package com.insightflow.dto;

import org.springframework.beans.factory.annotation.Value;

public interface EventTimelineProjection {
    @Value("#{target.date}")
    Object getDate();

    @Value("#{target.event_name != null ? target.event_name : (target.eventName != null ? target.eventName : target.eventname)}")
    Object getEventName();

    @Value("#{target.count}")
    Object getCount();
}
