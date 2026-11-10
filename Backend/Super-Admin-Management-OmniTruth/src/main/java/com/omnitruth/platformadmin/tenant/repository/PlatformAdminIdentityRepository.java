package com.omnitruth.platformadmin.tenant.repository;

import com.omnitruth.platformadmin.tenant.domain.PlatformAdminIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface PlatformAdminIdentityRepository extends JpaRepository<PlatformAdminIdentity, Long> {

    boolean existsByIssuerAndSubjectAndTenantIdAndAppRoleAndActiveTrue(
            String issuer,
            String subject,
            UUID tenantId,
            String appRole
    );
}