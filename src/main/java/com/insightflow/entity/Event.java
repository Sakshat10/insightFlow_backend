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
        @Index(name = "idx_events_project_id", columnList = "project_id"),
        @Index(name = "idx_events_tracking_key", columnList = "tracking_key"),
        @Index(name = "idx_events_session_id", columnList = "session_id"),
        @Index(name = "idx_events_event_name", columnList = "event_name"),
        @Index(name = "idx_events_created_at", columnList = "created_at")
    }
)
public class Event extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "tracking_key", nullable = false, length = 64)
    private String trackingKey;

    @Column(name = "session_id", length = 128)
    private String sessionId;

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

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "device_type", length = 20)
    private String deviceType;

    @Column(name = "browser", length = 50)
    private String browser;

    @Column(name = "properties", columnDefinition = "TEXT")
    private String properties;
}
