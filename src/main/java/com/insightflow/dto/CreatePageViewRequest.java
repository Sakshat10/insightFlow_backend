package com.insightflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePageViewRequest {

    private Long sessionId;

    private String url;

    @Size(max = 255)
    private String title;

    private String referrer;

    @Size(max = 45)
    private String ipAddress;

    private String userAgent;

    @Size(max = 100)
    private String country;

    @Size(max = 20)
    private String deviceType;

    @Size(max = 50)
    private String browser;
}
