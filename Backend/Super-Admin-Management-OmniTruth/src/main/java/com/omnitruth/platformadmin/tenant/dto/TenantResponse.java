package com.omnitruth.platformadmin.tenant.dto;

import java.time.Instant;
import java.util.UUID;

import com.omnitruth.platformadmin.tenant.domain.Tenant;
import com.omnitruth.platformadmin.tenant.domain.TenantStatus;

public record TenantResponse(UUID tenantId, String tenantCode, String companyName,
        String industry, TenantStatus status, Instant createdAt, Instant updatedAt) {
    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(tenant.getTenantId(), tenant.getTenantCode(), tenant.getCompanyName(),
                tenant.getIndustry(), tenant.getStatus(), tenant.getCreatedAt(), tenant.getUpdatedAt());
    }
}
