package com.omnitruth.tenant.management.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="tenant_api_endpoints", indexes={
    @Index(name="idx_tenant_api_endpoints_tenant", columnList="tenant_id")
})
public class TenantApiEndpoint {
    @Id @Column(name="endpoint_id", nullable=false, updatable=false)
    private UUID endpointId;
    @Column(name="tenant_id", nullable=false, updatable=false)
    private UUID tenantId;
    @Column(name="endpoint_name", nullable=false, length=160)
    private String endpointName;
    @Column(name="base_url", nullable=false, length=1000)
    private String baseUrl;
    @Column(name="http_method", nullable=false, length=10)
    private String httpMethod;
    @Column(name="auth_type", nullable=false, length=30)
    private String authType;
    @Column(name="secret_reference", length=300)
    private String secretReference;
    @Column(name="description", length=1000)
    private String description;
    @Column(name="enabled", nullable=false)
    private boolean enabled;
    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;
    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    public TenantApiEndpoint() {}
    public UUID getEndpointId(){return endpointId;} public void setEndpointId(UUID v){endpointId=v;}
    public UUID getTenantId(){return tenantId;} public void setTenantId(UUID v){tenantId=v;}
    public String getEndpointName(){return endpointName;} public void setEndpointName(String v){endpointName=v;}
    public String getBaseUrl(){return baseUrl;} public void setBaseUrl(String v){baseUrl=v;}
    public String getHttpMethod(){return httpMethod;} public void setHttpMethod(String v){httpMethod=v;}
    public String getAuthType(){return authType;} public void setAuthType(String v){authType=v;}
    public String getSecretReference(){return secretReference;} public void setSecretReference(String v){secretReference=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}
