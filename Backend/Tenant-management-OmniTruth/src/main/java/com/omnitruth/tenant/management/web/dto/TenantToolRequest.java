package com.omnitruth.tenant.management.web.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
public record TenantToolRequest(
    @NotBlank @Size(max=120) String toolName,
    @NotBlank @Size(max=1000) String description,
    @NotNull UUID endpointId,
    @NotBlank @Size(max=20000) String inputSchema,
    boolean enabled
) {}
