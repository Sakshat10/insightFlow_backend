package com.insightflow.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProjectRequest {

    @Size(min = 1, max = 100, message = "Project name must be between 1 and 100 characters")
    private String projectName;

    @Size(max = 255, message = "Domain must not exceed 255 characters")
    private String domain;

    private Integer projectStatus;
}

