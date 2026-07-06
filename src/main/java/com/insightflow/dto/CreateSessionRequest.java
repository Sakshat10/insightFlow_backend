package com.insightflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSessionRequest {

    @jakarta.validation.constraints.NotNull(message = "Project ID is required")
    private Integer projectId;

    @NotBlank(message = "Session ID is required")
    @Size(max = 128)
    private String sessionId;

    @Size(max = 45)
    private String ipAddress;

    @Size(max = 100)
    private String country;

    @Size(max = 100)
    private String city;

    @Size(max = 20)
    private String deviceType;

    @Size(max = 50)
    private String browser;

    @Size(max = 50)
    private String os;

    private String referrer;
}