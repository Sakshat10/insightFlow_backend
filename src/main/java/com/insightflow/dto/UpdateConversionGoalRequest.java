package com.insightflow.dto;

import com.insightflow.entity.ConversionGoalStatus;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateConversionGoalRequest {

    @Size(max = 255, message = "Goal name must not exceed 255 characters")
    private String name;

    private ConversionGoalStatus status;
}
