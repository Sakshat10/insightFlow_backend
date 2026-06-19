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
    name = "page_views",
    indexes = {
        @Index(name = "idx_page_views_project_id", columnList = "project_id"),
        @Index(name = "idx_page_views_tracking_key", columnList = "tracking_key"),
        @Index(name = "idx_page_views_session_id", columnList = "session_id"),
        @Index(name = "idx_page_views_url", columnList = "url(255)"),
        @Index(name = "idx_page_views_created_at", columnList = "created_at")
    }
)
public class PageView extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "tracking_key", nullable = false, length = 64)
    private String trackingKey;

    @Column(name = "session_id", length = 128)
    private String sessionId;

    @Column(name = "url", columnDefinition = "TEXT")
    private String url;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "referrer", columnDefinition = "TEXT")
    private String referrer;

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
}
