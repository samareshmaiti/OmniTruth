package com.omnitruth.tenant.management.web.dto;
import java.time.Instant;
import java.util.UUID;
public record TenantClientResponse(UUID clientId, String clientName, String contactEmail,
        String externalReference, String notes, String status, Instant createdAt, Instant updatedAt) {}
