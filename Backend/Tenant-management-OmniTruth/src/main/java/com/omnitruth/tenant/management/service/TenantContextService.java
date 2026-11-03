package com.omnitruth.tenant.management.service;

import com.omnitruth.tenant.management.domain.Tenant;
import com.omnitruth.tenant.management.repository.TenantIdentityMappingRepository;
import com.omnitruth.tenant.management.repository.TenantRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TenantContextService {
    private final TenantIdentityMappingRepository mappings;
    private final TenantRepository tenants;

    public TenantContextService(TenantIdentityMappingRepository mappings, TenantRepository tenants) {
        this.mappings = mappings;
        this.tenants = tenants;
    }

    @Transactional(readOnly = true)
    public Tenant requireTenant(Jwt jwt) {
        String entraTenantId = jwt.getClaimAsString("tid");
        if (entraTenantId == null || entraTenantId.isBlank()) {
            throw new IllegalArgumentException("Token is missing the Entra tenant claim.");
        }
        UUID tenantId = mappings.findByEntraTenantIdIgnoreCaseAndActiveTrue(entraTenantId)
                .orElseThrow(() -> new IllegalArgumentException("No active OmniTruth tenant is mapped to this Entra tenant."))
                .getTenantId();
        Tenant tenant = tenants.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Mapped OmniTruth tenant does not exist."));
        if (!"ACTIVE".equalsIgnoreCase(tenant.getStatus())) {
            throw new IllegalStateException("This OmniTruth tenant is not active.");
        }
        return tenant;
    }
}
