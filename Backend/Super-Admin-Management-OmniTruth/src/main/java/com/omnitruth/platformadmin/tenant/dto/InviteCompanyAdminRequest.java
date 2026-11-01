package com.omnitruth.platformadmin.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InviteCompanyAdminRequest(
        @NotBlank @Email @Size(max = 254) String email) { }
