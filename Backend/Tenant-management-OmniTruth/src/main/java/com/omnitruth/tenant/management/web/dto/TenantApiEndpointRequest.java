package com.omnitruth.tenant.management.web.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record TenantApiEndpointRequest(
    @NotBlank @Size(max=160) String endpointName,
    @NotBlank @Size(max=1000) String baseUrl,
    @NotBlank String httpMethod,
    @NotBlank String authType,
    @Size(max=300) String secretReference,
    @Size(max=1000) String description,
    boolean enabled
) {}
