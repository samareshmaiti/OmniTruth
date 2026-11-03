package com.omnitruth.tenant.management.repository;

import com.omnitruth.tenant.management.domain.TenantToolDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantToolDefinitionRepository extends JpaRepository<TenantToolDefinition, UUID> {
    List<TenantToolDefinition> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<TenantToolDefinition> findByToolIdAndTenantId(UUID toolId, UUID tenantId);
    long countByTenantId(UUID tenantId);
}
