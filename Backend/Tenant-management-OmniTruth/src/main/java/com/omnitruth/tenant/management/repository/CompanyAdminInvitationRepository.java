package com.omnitruth.tenant.management.repository;

import com.omnitruth.tenant.management.domain.CompanyAdminInvitation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CompanyAdminInvitationRepository
        extends JpaRepository<CompanyAdminInvitation, UUID> {

    boolean existsByEventId(UUID eventId);

    Optional<CompanyAdminInvitation> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from CompanyAdminInvitation invitation where invitation.tokenHash = :tokenHash")
    Optional<CompanyAdminInvitation> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
