
package com.omnitruth.platformadmin.tenant.dto;

import java.time.Instant;
import java.util.UUID;

public record InvitationAcceptedResponse(
        UUID eventId,
        UUID tenantId,
        String email,
        String role,
        String status,
        String message,
        Instant acceptedAt
) {
}
