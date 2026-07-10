package com.insightflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectSettingsRequest {

    @NotBlank(message = "Project name is required")
    @Size(min = 1, max = 100, message = "Project name must be between 1 and 100 characters")
    private String projectName;

    @Size(max = 255, message = "Domain must not exceed 255 characters")
    private String domain;

    @Size(max = 100, message = "Industry must not exceed 100 characters")
    private String industry;

    @NotBlank(message = "Timezone is required")
    @Size(max = 100, message = "Timezone must not exceed 100 characters")
    private String timezone;

    private Boolean pageviewTracking;
    private Boolean sessionRecording;
    private Boolean ipAnonymization;
    private Boolean botFiltering;
    private Boolean crossDomainTracking;
}
