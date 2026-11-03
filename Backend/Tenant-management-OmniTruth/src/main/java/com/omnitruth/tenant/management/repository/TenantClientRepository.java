package com.omnitruth.tenant.management.repository;

import com.omnitruth.tenant.management.domain.TenantClient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantClientRepository extends JpaRepository<TenantClient, UUID> {
    List<TenantClient> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<TenantClient> findByClientIdAndTenantId(UUID clientId, UUID tenantId);
    long countByTenantId(UUID tenantId);
}
