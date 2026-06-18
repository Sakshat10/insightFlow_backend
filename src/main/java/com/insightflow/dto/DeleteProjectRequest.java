package com.insightflow.dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteProjectRequest {

    @Enumerated(EnumType.STRING)
    private Integer projectStatus;
}
