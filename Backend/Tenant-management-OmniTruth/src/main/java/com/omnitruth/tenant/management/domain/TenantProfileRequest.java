package com.omnitruth.tenant.management.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantProfileRequest(

    @NotBlank
    @Size(max = 180)
    String companyName,

    @Size(max = 120)
    String industry

) {}
