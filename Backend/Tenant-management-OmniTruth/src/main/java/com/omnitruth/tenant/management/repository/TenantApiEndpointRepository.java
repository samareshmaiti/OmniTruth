package com.omnitruth.tenant.management.repository;

import com.omnitruth.tenant.management.domain.TenantApiEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantApiEndpointRepository extends JpaRepository<TenantApiEndpoint, UUID> {
    List<TenantApiEndpoint> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<TenantApiEndpoint> findByEndpointIdAndTenantId(UUID endpointId, UUID tenantId);
    long countByTenantId(UUID tenantId);
}
