package com.omnitruth.tenant.management.repository;

import com.omnitruth.tenant.management.domain.TenantIdentityMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TenantIdentityMappingRepository extends JpaRepository<TenantIdentityMapping, UUID> {
    Optional<TenantIdentityMapping> findByEntraTenantIdIgnoreCaseAndActiveTrue(String entraTenantId);
    Optional<TenantIdentityMapping> findByEntraTenantIdIgnoreCase(String entraTenantId);
    boolean existsByEntraTenantIdIgnoreCaseAndActiveTrue(String entraTenantId);
    Optional<TenantIdentityMapping> findByTenantIdAndActiveTrue(UUID tenantId);
    Optional<TenantIdentityMapping> findByTenantId(UUID tenantId);
}
