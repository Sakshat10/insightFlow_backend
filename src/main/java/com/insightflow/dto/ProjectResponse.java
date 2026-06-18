package com.insightflow.dto;

import com.insightflow.entity.Project;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProjectResponse {

    private final Integer id;
    private final Integer userId;
    private final String projectName;
    private final String domain;
    private final Integer projectStatus;
    private final String trackingKey;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static ProjectResponse from(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .userId(project.getUserId())
                .projectName(project.getProjectName())
                .domain(project.getDomain())
                .projectStatus(project.getProjectStatus())
                .trackingKey(project.getTrackingKey())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
