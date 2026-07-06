package com.insightflow.service;

import com.insightflow.dto.*;
import com.insightflow.entity.*;
import com.insightflow.exception.*;
import com.insightflow.repository.ApiKeyRepository;
import com.insightflow.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class ApiKeyService {

    private static final List<String> ALLOWED_PERMISSIONS = List.of("track", "identify", "alias");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ApiKeyRepository apiKeyRepository;
    private final ProjectRepository projectRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository, ProjectRepository projectRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public ApiKeyCreatedResponse createApiKey(CreateApiKeyRequest request, User currentUser) {
        if (request.getProjectId() == null) {
            throw new BadRequestException("projectId is required");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BadRequestException("name must not be blank");
        }
        if (request.getEnvironment() == null) {
            throw new BadRequestException("environment is required");
        }

        validateProjectAccess(request.getProjectId(), currentUser);

        String normalizedPermissions = normalizePermissions(request.getPermissions());

        // Generate key details
        String rawPrefix = getRawPrefixForEnv(request.getEnvironment());
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String rawKey = rawPrefix + secret;

        // Prefix for DB masking
        String prefixMask = rawPrefix + secret.substring(0, 4) + "..." + secret.substring(secret.length() - 4);
        String keyHash = hashKey(rawKey);

        ApiKey apiKey = ApiKey.builder()
                .projectId(request.getProjectId())
                .name(request.getName().trim())
                .keyPrefix(prefixMask)
                .keyHash(keyHash)
                .environment(request.getEnvironment())
                .status(ApiKeyStatus.ACTIVE)
                .permissions(normalizedPermissions)
                .requestCount(0L)
                .createdBy(currentUser.getId())
                .build();

        apiKey = apiKeyRepository.save(apiKey);

        log.info("API Key '{}' created for project ID {} by user {}", apiKey.getName(), apiKey.getProjectId(), currentUser.getUsername());

        return ApiKeyCreatedResponse.from(apiKey, rawKey);
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> getApiKeysByProject(Integer projectId, User currentUser) {
        if (projectId == null) {
            throw new BadRequestException("projectId is required");
        }
        validateProjectAccess(projectId, currentUser);

        List<ApiKey> keys = apiKeyRepository.findByProjectIdOrderByCreatedAtDescIdDesc(projectId);
        return keys.stream().map(ApiKeyResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ApiKeyResponse getApiKeyById(Integer id, User currentUser) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ApiKey", "id", id));
        validateProjectAccess(apiKey.getProjectId(), currentUser);
        return ApiKeyResponse.from(apiKey);
    }

    @Transactional
    public ApiKeyResponse revokeApiKey(Integer id, User currentUser) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ApiKey", "id", id));
        validateProjectAccess(apiKey.getProjectId(), currentUser);

        if (apiKey.getStatus() == ApiKeyStatus.ACTIVE) {
            apiKey.setStatus(ApiKeyStatus.REVOKED);
            apiKey.setRevokedAt(LocalDateTime.now());
            apiKey = apiKeyRepository.save(apiKey);
            log.info("API Key ID {} revoked by user {}", id, currentUser.getUsername());
        }

        return ApiKeyResponse.from(apiKey);
    }

    @Transactional
    public ApiKeyCreatedResponse rotateApiKey(Integer id, User currentUser) {
        ApiKey oldKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ApiKey", "id", id));
        validateProjectAccess(oldKey.getProjectId(), currentUser);

        if (oldKey.getStatus() != ApiKeyStatus.ACTIVE) {
            throw new BadRequestException("Only ACTIVE API keys can be rotated");
        }

        // 1. Revoke and rename the old key to free up unique name constraint
        String originalName = oldKey.getName();
        oldKey.setName(originalName + " (Rotated " + System.currentTimeMillis() + ")");
        oldKey.setStatus(ApiKeyStatus.REVOKED);
        oldKey.setRevokedAt(LocalDateTime.now());
        apiKeyRepository.saveAndFlush(oldKey);

        // 2. Generate new key with same config
        String rawPrefix = getRawPrefixForEnv(oldKey.getEnvironment());
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String rawKey = rawPrefix + secret;

        String prefixMask = rawPrefix + secret.substring(0, 4) + "..." + secret.substring(secret.length() - 4);
        String keyHash = hashKey(rawKey);

        ApiKey newKey = ApiKey.builder()
                .projectId(oldKey.getProjectId())
                .name(originalName)
                .keyPrefix(prefixMask)
                .keyHash(keyHash)
                .environment(oldKey.getEnvironment())
                .status(ApiKeyStatus.ACTIVE)
                .permissions(oldKey.getPermissions())
                .requestCount(0L)
                .createdBy(currentUser.getId())
                .build();

        newKey = apiKeyRepository.save(newKey);

        log.info("API Key rotated. Old key ID {} revoked, new key ID {} created", oldKey.getId(), newKey.getId());

        return ApiKeyCreatedResponse.from(newKey, rawKey);
    }

    @Transactional(readOnly = true)
    public ApiKeyStatsResponse getApiKeyStats(Integer projectId, User currentUser) {
        if (projectId == null) {
            throw new BadRequestException("projectId is required");
        }
        validateProjectAccess(projectId, currentUser);

        long total = apiKeyRepository.countByProjectId(projectId);
        long active = apiKeyRepository.countByProjectIdAndStatus(projectId, ApiKeyStatus.ACTIVE);
        long revoked = apiKeyRepository.countByProjectIdAndStatus(projectId, ApiKeyStatus.REVOKED);
        long requests = apiKeyRepository.sumRequestCountByProjectId(projectId);

        return ApiKeyStatsResponse.builder()
                .totalKeys(total)
                .activeKeys(active)
                .revokedKeys(revoked)
                .totalRequests(requests)
                .build();
    }

    private void validateProjectAccess(Integer projectId, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        if (!project.getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not have permission to access this project");
        }
    }

    private String normalizePermissions(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return null;
        }

        Set<String> normalized = new TreeSet<>();
        for (String perm : permissions) {
            if (perm == null || perm.trim().isEmpty()) {
                continue;
            }
            String trimmed = perm.trim().toLowerCase();
            if (!ALLOWED_PERMISSIONS.contains(trimmed)) {
                throw new BadRequestException("Invalid permission: " + trimmed);
            }
            normalized.add(trimmed);
        }

        if (normalized.isEmpty()) {
            return null;
        }

        return String.join(",", normalized);
    }

    private String getRawPrefixForEnv(ApiKeyEnvironment env) {
        return switch (env) {
            case PRODUCTION -> "if_live_pk_";
            case STAGING -> "if_test_pk_";
            case DEVELOPMENT -> "if_dev_pk_";
        };
    }

    private String hashKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
