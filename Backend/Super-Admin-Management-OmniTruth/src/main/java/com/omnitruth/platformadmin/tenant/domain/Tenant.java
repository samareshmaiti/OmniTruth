package com.omnitruth.platformadmin.tenant.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenants")
public class Tenant {
    @Id
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "tenant_code", nullable = false, unique = true, length = 80)
    private String tenantCode;

    @Column(name = "company_name", nullable = false, length = 180)
    private String companyName;

    @Column(name = "industry", length = 120)
    private String industry;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TenantStatus status;

    @Column(name = "created_by", nullable = false, length = 200)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Tenant() { }

    private Tenant(String tenantCode, String companyName, String industry, String createdBy) {
        this.tenantId = UUID.randomUUID();
        this.tenantCode = tenantCode;
        this.companyName = companyName;
        this.industry = industry;
        this.createdBy = createdBy;
        this.status = TenantStatus.PENDING;
    }

    public static Tenant create(String tenantCode, String companyName, String industry, String createdBy) {
        return new Tenant(tenantCode, companyName, industry, createdBy);
    }

    public void changeStatus(TenantStatus nextStatus) {
        if (this.status == nextStatus) return;
        boolean allowed = switch (this.status) {
            case PENDING -> nextStatus == TenantStatus.SSO_CONFIGURING || nextStatus == TenantStatus.SUSPENDED || nextStatus == TenantStatus.CLOSED;
            case SSO_CONFIGURING -> nextStatus == TenantStatus.PROVISIONING || nextStatus == TenantStatus.SUSPENDED || nextStatus == TenantStatus.CLOSED;
            case PROVISIONING -> nextStatus == TenantStatus.ACTIVE || nextStatus == TenantStatus.SUSPENDED || nextStatus == TenantStatus.CLOSED;
            case ACTIVE -> nextStatus == TenantStatus.SUSPENDED || nextStatus == TenantStatus.CLOSED;
            case SUSPENDED -> nextStatus == TenantStatus.ACTIVE || nextStatus == TenantStatus.CLOSED;
            case CLOSED -> false;
        };
        if (!allowed) throw new IllegalArgumentException("Tenant status transition is not allowed: " + status + " -> " + nextStatus);
        this.status = nextStatus;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public UUID getTenantId() { return tenantId; }
    public String getTenantCode() { return tenantCode; }
    public String getCompanyName() { return companyName; }
    public String getIndustry() { return industry; }
    public TenantStatus getStatus() { return status; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
