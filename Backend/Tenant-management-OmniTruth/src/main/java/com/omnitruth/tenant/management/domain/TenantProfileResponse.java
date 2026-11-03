package com.omnitruth.tenant.management.domain;

import java.time.Instant;
import java.util.UUID;

public record TenantProfileResponse(

    UUID tenantId,
    String tenantCode,
    String companyName,
    String industry,
    String status,
    Instant createdAt,
    Instant updatedAt

) {}
