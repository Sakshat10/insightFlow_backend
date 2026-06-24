package com.insightflow.dto;

import com.insightflow.entity.PageView;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PageViewResponse {

    private final Long id;
    private final Integer sessionId;
    private final String url;
    private final String title;
    private final String referrer;
    private final LocalDateTime createdAt;

    public static PageViewResponse from(PageView pv) {
        return PageViewResponse.builder()
                .id(pv.getId())
                .sessionId(pv.getSessionId())
                .url(pv.getUrl())
                .title(pv.getTitle())
                .referrer(pv.getReferrer())
                .createdAt(pv.getCreatedAt())
                .build();
    }
}