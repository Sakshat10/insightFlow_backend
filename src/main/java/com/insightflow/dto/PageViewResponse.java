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
    private final Long sessionId;
    private final String url;
    private final String title;
    private final String referrer;
    private final LocalDateTime createdAt;

    public static PageViewResponse from(PageView pageView) {
        return PageViewResponse.builder()
                .id(pageView.getId())
                .projectId(null)
                .sessionId(pageView.getSessionId())
                .url(pageView.getUrl())
                .title(pageView.getTitle())
                .referrer(pageView.getReferrer())
                .createdAt(pageView.getCreatedAt())
                .build();
    }

    public static PageViewResponse from(PageViewProjection pageView) {
        return PageViewResponse.builder()
                .id(pageView.getId())
                .projectId(pageView.getProjectId())
                .sessionId(pageView.getSessionId())
                .url(pageView.getUrl())
                .title(pageView.getTitle())
                .referrer(pageView.getReferrer())
                .createdAt(pageView.getCreatedAt())
                .build();
    }
}