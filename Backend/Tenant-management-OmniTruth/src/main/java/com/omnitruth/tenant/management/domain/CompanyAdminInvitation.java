
package com.omnitruth.tenant.management.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "company_admin_invitations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_company_admin_invitation_event_id",
            columnNames = "event_id"
        ),
        @UniqueConstraint(
            name = "uk_company_admin_invitation_token_hash",
            columnNames = "token_hash"
        )
    },
    indexes = {
        @Index(
            name = "idx_company_admin_invitation_tenant_id",
            columnList = "tenant_id"
        ),
        @Index(
            name = "idx_company_admin_invitation_email",
            columnList = "email"
        ),
        @Index(
            name = "idx_company_admin_invitation_status",
            columnList = "status"
        )
    }
)
public class CompanyAdminInvitation {

    @Id
    @Column(name = "invitation_id", nullable = false, updatable = false)
    private UUID invitationId;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "requested_role", nullable = false, length = 50)
    private String requestedRole;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

}