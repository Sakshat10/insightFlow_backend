package com.insightflow.dto;

import com.insightflow.entity.Funnel;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FunnelResponse {

    private Integer id;
    private Integer projectId;
    private String name;
    private String description;
    private Integer createdBy;
    private List<FunnelStepResponse> steps;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FunnelResponse from(Funnel funnel) {
        if (funnel == null) {
            return null;
        }
        List<FunnelStepResponse> stepResponses = funnel.getSteps() == null ? Collections.emptyList() :
                funnel.getSteps().stream()
                        .map(FunnelStepResponse::from)
                        .toList();

        return FunnelResponse.builder()
                .id(funnel.getId())
                .projectId(funnel.getProjectId())
                .name(funnel.getName())
                .description(funnel.getDescription())
                .createdBy(funnel.getCreatedBy())
                .steps(stepResponses)
                .createdAt(funnel.getCreatedAt())
                .updatedAt(funnel.getUpdatedAt())
                .build();
    }
}
