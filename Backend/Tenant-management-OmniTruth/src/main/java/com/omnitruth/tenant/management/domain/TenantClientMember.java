package com.omnitruth.tenant.management.domain;

import com.omnitruth.tenant.management.utility.ClientUserRole;
import com.omnitruth.tenant.management.utility.IdentityProvider;
import jakarta.persistence.*;
import lombok.*;


import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Table(name = "tenant_client_members", indexes = {
    @Index(name = "idx_client_members_tenant", columnList = "tenant_id"),
    @Index(name = "idx_client_members_client", columnList = "client_id"),
    @Index(name = "idx_client_members_email", columnList = "email")
})
public class TenantClientMember {

    @Id
    @Column(name = "member_id", nullable = false, updatable = false)
    private UUID memberId;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Column(name = "full_name", length = 180)
    private String fullName;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_provider", length = 30)
    private IdentityProvider identityProvider;

    @Column(name = "entra_tenant_id", length = 36)
    private String entraTenantId;

    @Column(name = "entra_object_id", length = 36)
    private String entraObjectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_role", nullable = false, length = 40)
    private ClientUserRole appRole;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}