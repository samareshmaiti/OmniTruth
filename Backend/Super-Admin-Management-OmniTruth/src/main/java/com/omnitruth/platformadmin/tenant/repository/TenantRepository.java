package com.omnitruth.platformadmin.tenant.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.omnitruth.platformadmin.tenant.domain.Tenant;
import com.omnitruth.platformadmin.tenant.domain.TenantStatus;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    boolean existsByTenantCodeIgnoreCase(String tenantCode);
    Optional<Tenant> findByTenantCodeIgnoreCase(String tenantCode);
    org.springframework.data.domain.Page<Tenant> findAllByStatus(TenantStatus status, org.springframework.data.domain.Pageable pageable);
}
