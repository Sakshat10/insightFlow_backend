package com.insightflow.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "projects",
    indexes = {
        @Index(name = "idx_projects_user_id", columnList = "user_id"),
        @Index(name = "idx_projects_tracking_key", columnList = "tracking_key"),
        @Index(name = "idx_projects_status", columnList = "project_status")
    }
)
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "project_name", nullable = false, length = 255)
    private String projectName;

    @Column(name = "domain", length = 255)
    private String domain;

    @Column(name = "project_status", nullable = false)
    @Builder.Default
    private Integer projectStatus = 1;

    @Column(name = "tracking_key", nullable = false, unique = true, length = 255)
    private String trackingKey;

    // @Column(name = "is_deleted", nullable = false)
    // @Builder.Default
    // private Boolean isDeleted = false;
}
