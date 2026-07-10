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

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "timezone", length = 100)
    @Builder.Default
    private String timezone = "UTC";

    @Column(name = "pageview_tracking")
    @Builder.Default
    private Boolean pageviewTracking = true;

    @Column(name = "session_recording")
    @Builder.Default
    private Boolean sessionRecording = false;

    @Column(name = "ip_anonymization")
    @Builder.Default
    private Boolean ipAnonymization = true;

    @Column(name = "bot_filtering")
    @Builder.Default
    private Boolean botFiltering = true;

    @Column(name = "cross_domain_tracking")
    @Builder.Default
    private Boolean crossDomainTracking = false;
}
