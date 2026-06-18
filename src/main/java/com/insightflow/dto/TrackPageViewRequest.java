package com.insightflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrackPageViewRequest {

    @NotBlank(message = "Tracking key is required")
    private String trackingKey;

    @Size(max = 128)
    private String sessionId;

    private String url;

    @Size(max = 255)
    private String title;

    private String referrer;
}
