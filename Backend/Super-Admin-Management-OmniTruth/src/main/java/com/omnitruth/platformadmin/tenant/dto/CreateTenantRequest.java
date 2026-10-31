package com.omnitruth.platformadmin.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank @Size(max = 80) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_-]*", message = "Use letters, numbers, underscores or hyphens") String tenantCode,
        @NotBlank @Size(max = 180) String companyName,
        @Size(max = 120) String industry) { }
