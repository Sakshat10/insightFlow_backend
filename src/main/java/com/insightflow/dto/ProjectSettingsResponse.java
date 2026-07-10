package com.insightflow.dto;

import com.insightflow.entity.Project;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectSettingsResponse {
    private final Integer projectId;
    private final String projectName;
    private final String domain;
    private final String industry;
    private final String timezone;
    private final Boolean pageviewTracking;
    private final Boolean sessionRecording;
    private final Boolean ipAnonymization;
    private final Boolean botFiltering;
    private final Boolean crossDomainTracking;

    public static ProjectSettingsResponse from(Project project) {
        return ProjectSettingsResponse.builder()
                .projectId(project.getId())
                .projectName(project.getProjectName())
                .domain(project.getDomain())
                .industry(project.getIndustry())
                .timezone(project.getTimezone())
                .pageviewTracking(project.getPageviewTracking())
                .sessionRecording(project.getSessionRecording())
                .ipAnonymization(project.getIpAnonymization())
                .botFiltering(project.getBotFiltering())
                .crossDomainTracking(project.getCrossDomainTracking())
                .build();
    }
}
