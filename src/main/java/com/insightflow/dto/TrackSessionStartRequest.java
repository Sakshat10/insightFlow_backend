package com.insightflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrackSessionStartRequest {

    @NotBlank(message = "Session ID is required")
    @Size(max = 128)
    private String sessionId;

    @NotBlank(message = "Visitor ID is required")
    @Size(max = 128)
    private String visitorId;

    private String referrer;

    private String userAgent;
}
