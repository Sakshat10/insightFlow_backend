package com.insightflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateEventRequest {

    private Long sessionId;

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

    private String properties;

    private Boolean isConversion;
}
