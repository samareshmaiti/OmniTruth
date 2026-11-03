package com.omnitruth.tenant.management.web.dto.onboarding;

import jakarta.validation.constraints.NotBlank;

public record AcceptInvitationRequest(
        @NotBlank String token
) { }
