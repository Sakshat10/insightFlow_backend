package com.insightflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrackSessionEndRequest {

    @NotBlank(message = "Tracking key is required")
    private String trackingKey;

    @NotBlank(message = "Session ID is required")
    @Size(max = 128)
    private String sessionId;

    private Integer duration;

    private Integer pageViewCount;
}
