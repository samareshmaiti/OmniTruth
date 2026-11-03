package com.omnitruth.tenant.management.web.dto.onboarding;

import jakarta.validation.constraints.NotBlank;

public record CompleteOnboardingRequest(
        @NotBlank String invitationToken
) { }
