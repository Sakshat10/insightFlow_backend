package com.insightflow.service;

import com.insightflow.entity.ApiKey;
import com.insightflow.entity.ApiKeyStatus;
import com.insightflow.exception.BadRequestException;
import com.insightflow.repository.ApiKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@Service
public class ApiKeyValidationService {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyValidationService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Transactional
    public ApiKey validateAndIncrement(String rawKey, String requiredPermission) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new BadRequestException("API key is required");
        }

        String hash = hashKey(rawKey);
        ApiKey apiKey = apiKeyRepository.findByKeyHash(hash)
                .orElseThrow(() -> new BadRequestException("Invalid API key"));

        if (apiKey.getStatus() != ApiKeyStatus.ACTIVE) {
            throw new BadRequestException("API key has been revoked");
        }

        if (requiredPermission != null) {
            boolean hasPermission = false;
            if (apiKey.getPermissions() != null && !apiKey.getPermissions().isBlank()) {
                hasPermission = Arrays.stream(apiKey.getPermissions().split(","))
                        .map(String::trim)
                        .anyMatch(p -> p.equalsIgnoreCase(requiredPermission));
            }
            if (!hasPermission) {
                throw new BadRequestException("API key lacks required permission: " + requiredPermission);
            }
        }

        // Atomic increment
        apiKeyRepository.incrementRequestCount(apiKey.getId());

        return apiKey;
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
