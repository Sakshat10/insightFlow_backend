package com.insightflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateFunnelRequest {

    @NotNull(message = "projectId is required")
    private Integer projectId;

    @NotBlank(message = "name must not be blank")
    private String name;

    private String description;

    @NotEmpty(message = "steps are required")
    private List<FunnelStepRequest> steps;
}
