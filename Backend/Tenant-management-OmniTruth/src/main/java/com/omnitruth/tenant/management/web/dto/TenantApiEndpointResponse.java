package com.omnitruth.tenant.management.web.dto;
import java.time.Instant;
import java.util.UUID;
public record TenantApiEndpointResponse(UUID endpointId, String endpointName, String baseUrl,
        String httpMethod, String authType, String secretReference, String description,
        boolean enabled, Instant createdAt, Instant updatedAt) {}
