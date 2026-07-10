package com.insightflow.dto;

import com.insightflow.entity.ConversionGoal;
import com.insightflow.entity.ConversionGoalStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ConversionGoalResponse {
    private final Integer id;
    private final Integer projectId;
    private final String name;
    private final String eventName;
    private final ConversionGoalStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static ConversionGoalResponse from(ConversionGoal goal) {
        if (goal == null) return null;
        return ConversionGoalResponse.builder()
                .id(goal.getId())
                .projectId(goal.getProjectId())
                .name(goal.getName())
                .eventName(goal.getEventName())
                .status(goal.getStatus())
                .createdAt(goal.getCreatedAt())
                .updatedAt(goal.getUpdatedAt())
                .build();
    }
}
