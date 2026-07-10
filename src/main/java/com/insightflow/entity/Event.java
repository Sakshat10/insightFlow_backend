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
    name = "events",
    indexes = {
        @Index(name = "idx_events_session_id", columnList = "session_id"),
        @Index(name = "idx_events_event_name", columnList = "event_name"),
        @Index(name = "idx_events_created_at", columnList = "created_at")
    }
)
public class Event extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "event_name", nullable = false, length = 100)
    private String eventName;

    @Column(name = "event_category", length = 100)
    private String eventCategory;

    @Column(name = "event_label", length = 255)
    private String eventLabel;

    @Column(name = "event_value", length = 255)
    private String eventValue;

    @Column(name = "url", columnDefinition = "TEXT")
    private String url;

    @Column(name = "properties", columnDefinition = "TEXT")
    private String properties;

    @Column(name = "is_conversion")
    @Builder.Default
    private Boolean isConversion = false;
}
