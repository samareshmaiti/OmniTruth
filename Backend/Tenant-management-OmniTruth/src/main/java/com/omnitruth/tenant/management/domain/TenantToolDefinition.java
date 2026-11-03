package com.omnitruth.tenant.management.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="tenant_tool_definitions", indexes={
    @Index(name="idx_tenant_tools_tenant", columnList="tenant_id")
})
public class TenantToolDefinition {
    @Id @Column(name="tool_id", nullable=false, updatable=false)
    private UUID toolId;
    @Column(name="tenant_id", nullable=false, updatable=false)
    private UUID tenantId;
    @Column(name="endpoint_id", nullable=false)
    private UUID endpointId;
    @Column(name="tool_name", nullable=false, length=120)
    private String toolName;
    @Column(name="description", nullable=false, length=1000)
    private String description;
    @Column(name="input_schema", nullable=false, columnDefinition="TEXT")
    private String inputSchema;
    @Column(name="enabled", nullable=false)
    private boolean enabled;
    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;
    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    public TenantToolDefinition() {}
    public UUID getToolId(){return toolId;} public void setToolId(UUID v){toolId=v;}
    public UUID getTenantId(){return tenantId;} public void setTenantId(UUID v){tenantId=v;}
    public UUID getEndpointId(){return endpointId;} public void setEndpointId(UUID v){endpointId=v;}
    public String getToolName(){return toolName;} public void setToolName(String v){toolName=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getInputSchema(){return inputSchema;} public void setInputSchema(String v){inputSchema=v;}
    public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}
