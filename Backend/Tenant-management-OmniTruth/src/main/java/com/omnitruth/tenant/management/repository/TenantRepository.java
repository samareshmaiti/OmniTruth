
package com.omnitruth.tenant.management.repository;


import com.omnitruth.tenant.management.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByTenantCodeIgnoreCase(String tenantCode);

    boolean existsByTenantCodeIgnoreCase(String tenantCode);

    Optional<Tenant> findByTenantIdAndTenantCode(
            UUID tenantId,
            String tenantCode
    );
}
