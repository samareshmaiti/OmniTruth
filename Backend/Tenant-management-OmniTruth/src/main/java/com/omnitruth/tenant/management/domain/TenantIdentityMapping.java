package com.omnitruth.tenant.management.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_identity_mappings",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_tenant_identity_entra_tid", columnNames = "entra_tenant_id"),
           @UniqueConstraint(name = "uk_tenant_identity_tenant", columnNames = "tenant_id")
       })
public class TenantIdentityMapping {
    @Id
    @Column(name = "mapping_id", nullable = false, updatable = false)
    private UUID mappingId;
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;
    @Column(name = "entra_tenant_id", nullable = false, updatable = false, length = 36)
    private String entraTenantId;
    @Column(name = "active", nullable = false)
    private boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public TenantIdentityMapping() {}
    public UUID getMappingId() { return mappingId; }
    public void setMappingId(UUID mappingId) { this.mappingId = mappingId; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getEntraTenantId() { return entraTenantId; }
    public void setEntraTenantId(String entraTenantId) { this.entraTenantId = entraTenantId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
