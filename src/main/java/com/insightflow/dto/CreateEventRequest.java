package com.insightflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateEventRequest {

    @NotBlank(message = "Tracking key is required")
    private String trackingKey;

    @Size(max = 128)
    private String sessionId;

    @NotBlank(message = "Event name is required")
    @Size(max = 100)
    private String eventName;

    @Size(max = 100)
    private String eventCategory;

    @Size(max = 255)
    private String eventLabel;

    @Size(max = 255)
    private String eventValue;

    private String url;

    @Size(max = 45)
    private String ipAddress;

    private String userAgent;

    @Size(max = 100)
    private String country;

    @Size(max = 20)
    private String deviceType;

    @Size(max = 50)
    private String browser;

    private String properties;
}
