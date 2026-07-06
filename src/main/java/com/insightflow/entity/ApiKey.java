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
@Table(name = "api_keys")
public class ApiKey extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "key_prefix", nullable = false, length = 100)
    private String keyPrefix;

    @Column(name = "key_hash", nullable = false, unique = true, length = 255)
    private String keyHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false, length = 20)
    private ApiKeyEnvironment environment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApiKeyStatus status;

    @Column(name = "permissions", length = 255)
    private String permissions;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "request_count", nullable = false)
    private Long requestCount;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
}
