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
        @Index(name = "idx_page_views_session_id", columnList = "session_id"),
        @Index(name = "idx_page_views_url", columnList = "page_url(255)"),
        @Index(name = "idx_page_views_created_at", columnList = "created_at")
    }
)
public class PageView extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Integer sessionId;

    @Column(name = "page_url", columnDefinition = "TEXT")
    private String url;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "referrer", length = 255)
    private String referrer;
}