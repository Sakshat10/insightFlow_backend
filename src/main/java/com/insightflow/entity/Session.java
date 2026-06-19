package com.insightflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "sessions",
    indexes = {
        @Index(name = "idx_sessions_project_id", columnList = "project_id"),
        @Index(name = "idx_sessions_tracking_key", columnList = "tracking_key"),
        @Index(name = "idx_sessions_session_id", columnList = "session_id"),
        @Index(name = "idx_sessions_started_at", columnList = "started_at")
    }
)
public class Session extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "tracking_key", nullable = false, length = 64)
    private String trackingKey;

    @Column(name = "session_id", nullable = false, unique = true, length = 128)
    private String sessionId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "device_type", length = 20)
    private String deviceType;

    @Column(name = "browser", length = 50)
    private String browser;

    @Column(name = "os", length = 50)
    private String os;

    @Column(name = "referrer", columnDefinition = "TEXT")
    private String referrer;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "page_view_count")
    @Builder.Default
    private Integer pageViewCount = 0;

    @Column(name = "is_bounce")
    @Builder.Default
    private Boolean isBounce = true;
}
