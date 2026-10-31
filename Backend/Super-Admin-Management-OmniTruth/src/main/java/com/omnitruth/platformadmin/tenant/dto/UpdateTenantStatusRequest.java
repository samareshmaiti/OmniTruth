package com.omnitruth.platformadmin.tenant.dto;

import com.omnitruth.platformadmin.tenant.domain.TenantStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTenantStatusRequest(@NotNull TenantStatus status) { }
