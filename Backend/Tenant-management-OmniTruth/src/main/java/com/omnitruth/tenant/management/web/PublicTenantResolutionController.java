package com.omnitruth.tenant.management.web;

import com.omnitruth.tenant.management.domain.Tenant;
import com.omnitruth.tenant.management.repository.TenantIdentityMappingRepository;
import com.omnitruth.tenant.management.repository.TenantRepository;
import com.omnitruth.tenant.management.web.dto.PublicTenantResolutionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/public/v1/tenants")
public class PublicTenantResolutionController {
    private final TenantRepository tenants;
    private final TenantIdentityMappingRepository mappings;

    public PublicTenantResolutionController(TenantRepository tenants, TenantIdentityMappingRepository mappings) {
        this.tenants = tenants;
        this.mappings = mappings;
    }

    @GetMapping("/resolve/{tenantSlug}")
    public PublicTenantResolutionResponse resolve(@PathVariable String tenantSlug) {
        Tenant tenant = tenants.findByTenantCodeIgnoreCase(tenantSlug)
                .filter(value -> "ACTIVE".equalsIgnoreCase(value.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active tenant not found."));
        var mapping = mappings.findByTenantIdAndActiveTrue(tenant.getTenantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant SSO is not configured."));
        return new PublicTenantResolutionResponse(
                tenant.getTenantId(),
                tenant.getTenantCode().toLowerCase(java.util.Locale.ROOT),
                tenant.getCompanyName(),
                mapping.getEntraTenantId(),
                tenant.getStatus()
        );
    }

}
