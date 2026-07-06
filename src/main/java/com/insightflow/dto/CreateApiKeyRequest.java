package com.insightflow.dto;

import com.insightflow.entity.ApiKeyEnvironment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateApiKeyRequest {

    @NotNull(message = "projectId is required")
    private Integer projectId;

    @NotBlank(message = "name must not be blank")
    private String name;

    @NotNull(message = "environment is required")
    private ApiKeyEnvironment environment;

    private List<String> permissions;
}
