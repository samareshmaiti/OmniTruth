package com.omnitruth.tenant.management.repository;

import com.omnitruth.tenant.management.domain.TenantUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantUserRepository extends JpaRepository<TenantUser, UUID> {

    Optional<TenantUser> findByEntraTenantIdIgnoreCaseAndEntraObjectIdIgnoreCase(
            String entraTenantId, String entraObjectId);

    Optional<TenantUser> findByEntraTenantIdIgnoreCaseAndEntraObjectIdIgnoreCaseAndStatus(
            String entraTenantId, String entraObjectId, String status);
}
