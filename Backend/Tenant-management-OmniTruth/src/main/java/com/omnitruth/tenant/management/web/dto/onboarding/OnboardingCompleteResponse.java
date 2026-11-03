package com.omnitruth.tenant.management.web.dto.onboarding;

import java.util.UUID;

public record OnboardingCompleteResponse(
        UUID tenantId,
        String tenantCode,
        String companyName,
        String entraTenantId,
        String onboardingStatus,
        String message
) { }
