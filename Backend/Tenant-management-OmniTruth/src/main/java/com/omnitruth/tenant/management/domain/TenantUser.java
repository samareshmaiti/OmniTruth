package com.omnitruth.tenant.management.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;
//This class is for tenants. While getting an invite from super admin
//this table will save the tenant details
@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "tenant_users",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_tenant_user_entra_identity",
            columnNames = {"entra_tenant_id", "entra_object_id"}
        )
    },
    indexes = {
        @Index(name = "idx_tenant_users_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_tenant_users_status", columnList = "status")
    }
)
public class TenantUser {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "entra_tenant_id", nullable = false, length = 36, updatable = false)
    private String entraTenantId;

    @Column(name = "entra_object_id", nullable = false, length = 36, updatable = false)
    private String entraObjectId;

    // The invitation/contact address can differ from the Entra sign-in name.
    @Column(name = "invitation_email", nullable = false, length = 254)
    private String invitationEmail;

    @Column(name = "app_role", nullable = false, length = 50)
    private String appRole;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
