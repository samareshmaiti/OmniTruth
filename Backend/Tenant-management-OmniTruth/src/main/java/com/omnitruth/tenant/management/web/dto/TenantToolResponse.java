package com.omnitruth.tenant.management.web.dto;
import java.time.Instant;
import java.util.UUID;
public record TenantToolResponse(UUID toolId, UUID endpointId, String toolName, String description,
        String inputSchema, boolean enabled, Instant createdAt, Instant updatedAt) {}
