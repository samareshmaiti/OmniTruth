package com.omnitruth.tenant.management.web.dto;
import java.util.UUID;
public record TenantDashboardResponse(
    TenantSummary tenant,
    long totalClients,
    long activeClients,
    long totalApiEndpoints,
    long enabledApiEndpoints,
    long totalTools,
    long enabledTools
) {
    public record TenantSummary(UUID tenantId, String tenantCode, String companyName, String status) {}
}
