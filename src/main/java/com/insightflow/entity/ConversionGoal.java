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
    name = "conversion_goals",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_conversion_goal_project_event", columnNames = {"project_id", "event_name"})
    },
    indexes = {
        @Index(name = "idx_conversion_goals_project_id", columnList = "project_id"),
        @Index(name = "idx_conversion_goals_project_status", columnList = "project_id, status"),
        @Index(name = "idx_conversion_goals_lookup", columnList = "project_id, event_name, status")
    }
)
public class ConversionGoal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "event_name", nullable = false, length = 255)
    private String eventName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ConversionGoalStatus status = ConversionGoalStatus.ACTIVE;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;
}
