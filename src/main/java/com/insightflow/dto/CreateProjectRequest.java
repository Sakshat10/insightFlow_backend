package com.insightflow.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProjectRequest {
    private String projectName;
    private String domain;
}
