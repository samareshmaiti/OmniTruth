package com.omnitruth.tenant.management.domain;

import com.omnitruth.tenant.management.utility.ClientUserRole;
import com.omnitruth.tenant.management.utility.TenantClientType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(name = "tenant_clients", indexes = {
    @Index(name = "idx_tenant_clients_tenant", columnList = "tenant_id"),
    @Index(name = "idx_tenant_clients_name", columnList = "client_name")
})
public class TenantClient {
    @Id
    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;
    @Column(name = "client_name", nullable = false, length = 180)
    private String clientName;
    @Column(name = "contact_email", length = 254)
    private String contactEmail;
    @Column(name = "external_reference", length = 120)
    private String externalReference;
    @Column(name = "notes", length = 2000)
    private String notes;
    @Column(name = "status", nullable = false, length = 20)
    private String status;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "client_type",nullable = false)
    private TenantClientType tenantClientType;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_role",nullable = false)
    private ClientUserRole appRole;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (clientId == null) {
            clientId = UUID.randomUUID();
        }

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
