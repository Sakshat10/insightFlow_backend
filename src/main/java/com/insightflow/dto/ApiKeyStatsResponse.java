package com.insightflow.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyStatsResponse {

    private long totalKeys;
    private long activeKeys;
    private long revokedKeys;
    private long totalRequests;
}
