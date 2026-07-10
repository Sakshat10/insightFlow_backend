package com.insightflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateConversionGoalRequest {

    @NotNull(message = "Project ID is required")
    private Integer projectId;

    @NotBlank(message = "Goal name is required")
    @Size(max = 255, message = "Goal name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Event name is required")
    @Size(max = 255, message = "Event name must not exceed 255 characters")
    private String eventName;
}
