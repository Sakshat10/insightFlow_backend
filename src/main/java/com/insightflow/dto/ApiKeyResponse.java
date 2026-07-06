package com.insightflow.dto;

import com.insightflow.entity.ApiKey;
import com.insightflow.entity.ApiKeyEnvironment;
import com.insightflow.entity.ApiKeyStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyResponse {

    private Integer id;
    private Integer projectId;
    private String name;
    private String keyPrefix;
    private ApiKeyEnvironment environment;
    private ApiKeyStatus status;
    private List<String> permissions;
    private LocalDateTime lastUsedAt;
    private Long requestCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime revokedAt;

    public static ApiKeyResponse from(ApiKey apiKey) {
        if (apiKey == null) {
            return null;
        }

        List<String> perms = Collections.emptyList();
        if (apiKey.getPermissions() != null && !apiKey.getPermissions().isBlank()) {
            perms = Arrays.stream(apiKey.getPermissions().split(","))
                    .map(String::trim)
                    .toList();
        }

        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .projectId(apiKey.getProjectId())
                .name(apiKey.getName())
                .keyPrefix(apiKey.getKeyPrefix())
                .environment(apiKey.getEnvironment())
                .status(apiKey.getStatus())
                .permissions(perms)
                .lastUsedAt(apiKey.getLastUsedAt())
                .requestCount(apiKey.getRequestCount())
                .createdAt(apiKey.getCreatedAt())
                .updatedAt(apiKey.getUpdatedAt())
                .revokedAt(apiKey.getRevokedAt())
                .build();
    }
}
