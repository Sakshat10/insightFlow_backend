package com.insightflow.dto;

import com.insightflow.entity.FunnelStep;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FunnelStepResponse {

    private Integer id;
    private Integer stepOrder;
    private String eventName;
    private LocalDateTime createdAt;

    public static FunnelStepResponse from(FunnelStep step) {
        if (step == null) {
            return null;
        }
        return FunnelStepResponse.builder()
                .id(step.getId())
                .stepOrder(step.getStepOrder())
                .eventName(step.getEventName())
                .createdAt(step.getCreatedAt())
                .build();
    }
}
