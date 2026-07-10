package com.insightflow.service;

import com.insightflow.dto.LiveActivityResponse;
import com.insightflow.event.TrackingActivityCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class LiveActivityStreamListener {

    private final LiveActivityStreamService liveActivityStreamService;

    public LiveActivityStreamListener(LiveActivityStreamService liveActivityStreamService) {
        this.liveActivityStreamService = liveActivityStreamService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTrackingActivityCreated(TrackingActivityCreatedEvent event) {
        LiveActivityResponse response = LiveActivityResponse.builder()
                .activityId(event.getActivityId())
                .type(event.getType())
                .projectId(event.getProjectId())
                .sourceId(event.getSourceId())
                .visitorId(event.getVisitorId())
                .sessionId(event.getSessionId())
                .eventName(event.getEventName())
                .title(event.getTitle())
                .url(event.getUrl())
                .country(event.getCountry())
                .browser(event.getBrowser())
                .deviceType(event.getDeviceType())
                .isConversion(event.getIsConversion())
                .timestamp(event.getActivityTimestamp())
                .build();

        liveActivityStreamService.publish(event.getProjectId(), response);
    }
}
