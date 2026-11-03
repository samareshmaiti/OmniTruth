package com.omnitruth.tenant.management.web.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record TenantClientRequest(
    @NotBlank @Size(max=180) String clientName,
    @Email @Size(max=254) String contactEmail,
    @Size(max=120) String externalReference,
    @Size(max=2000) String notes,
    @NotBlank String status
) {}
