package com.insightflow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FunnelStepRequest {

    @NotNull(message = "stepOrder is required")
    @Min(value = 1, message = "stepOrder must be positive")
    private Integer stepOrder;

    @NotBlank(message = "eventName must not be blank")
    private String eventName;
}
