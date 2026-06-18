package com.insightflow.dto;

import com.insightflow.entity.PageView;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PageViewResponse {

    private final Long id;
    private final Integer projectId;
    private final String trackingKey;
    private final String sessionId;
    private final String url;
    private final String title;
    private final String referrer;
    private final String country;
    private final String deviceType;
    private final String browser;
    private final LocalDateTime createdAt;

    public static PageViewResponse from(PageView pv) {
        return PageViewResponse.builder()
                .id(pv.getId())
                .projectId(pv.getProjectId())
                .trackingKey(pv.getTrackingKey())
                .sessionId(pv.getSessionId())
                .url(pv.getUrl())
                .title(pv.getTitle())
                .referrer(pv.getReferrer())
                .country(pv.getCountry())
                .deviceType(pv.getDeviceType())
                .browser(pv.getBrowser())
                .createdAt(pv.getCreatedAt())
                .build();
    }
}
