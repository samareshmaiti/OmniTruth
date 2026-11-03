package com.omnitruth.tenant.management.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_clients", indexes = {
    @Index(name = "idx_tenant_clients_tenant", columnList = "tenant_id"),
    @Index(name = "idx_tenant_clients_name", columnList = "client_name")
})
public class TenantClient {
    @Id @Column(name="client_id", nullable=false, updatable=false)
    private UUID clientId;
    @Column(name="tenant_id", nullable=false, updatable=false)
    private UUID tenantId;
    @Column(name="client_name", nullable=false, length=180)
    private String clientName;
    @Column(name="contact_email", length=254)
    private String contactEmail;
    @Column(name="external_reference", length=120)
    private String externalReference;
    @Column(name="notes", length=2000)
    private String notes;
    @Column(name="status", nullable=false, length=20)
    private String status;
    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;
    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    public TenantClient() {}
    public UUID getClientId(){return clientId;} public void setClientId(UUID v){clientId=v;}
    public UUID getTenantId(){return tenantId;} public void setTenantId(UUID v){tenantId=v;}
    public String getClientName(){return clientName;} public void setClientName(String v){clientName=v;}
    public String getContactEmail(){return contactEmail;} public void setContactEmail(String v){contactEmail=v;}
    public String getExternalReference(){return externalReference;} public void setExternalReference(String v){externalReference=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}
