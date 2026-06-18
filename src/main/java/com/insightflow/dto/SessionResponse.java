package com.insightflow.dto;

import com.insightflow.entity.Session;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SessionResponse {

    private final Long id;
    private final Integer projectId;
    private final String trackingKey;
    private final String sessionId;
    private final String ipAddress;
    private final String country;
    private final String city;
    private final String deviceType;
    private final String browser;
    private final String os;
    private final String referrer;
    private final LocalDateTime startedAt;
    private final LocalDateTime endedAt;
    private final Integer duration;
    private final Integer pageViewCount;
    private final Boolean isBounce;
    private final LocalDateTime createdAt;

    public static SessionResponse from(Session session) {
        return SessionResponse.builder()
                .id(session.getId())
                .projectId(session.getProjectId())
                .trackingKey(session.getTrackingKey())
                .sessionId(session.getSessionId())
                .ipAddress(session.getIpAddress())
                .country(session.getCountry())
                .city(session.getCity())
                .deviceType(session.getDeviceType())
                .browser(session.getBrowser())
                .os(session.getOs())
                .referrer(session.getReferrer())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .duration(session.getDuration())
                .pageViewCount(session.getPageViewCount())
                .isBounce(session.getIsBounce())
                .createdAt(session.getCreatedAt())
                .build();
    }
}
