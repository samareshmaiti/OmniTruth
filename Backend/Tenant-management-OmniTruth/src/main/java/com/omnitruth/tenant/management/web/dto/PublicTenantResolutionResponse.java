package com.omnitruth.tenant.management.web.dto;
import java.util.UUID;
public record PublicTenantResolutionResponse(
    UUID tenantId,
    String tenantSlug,
    String companyName,
    String entraTenantId,
    String status
) {}
