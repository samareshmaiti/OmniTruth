
package com.omnitruth.tenant.management.service;

import com.omnitruth.tenant.management.domain.CompanyAdminInvitation;
import com.omnitruth.tenant.management.domain.CompanyAdminInvitationRequestedEvent;
import com.omnitruth.tenant.management.domain.Tenant;
import com.omnitruth.tenant.management.repository.CompanyAdminInvitationRepository;
import com.omnitruth.tenant.management.repository.TenantRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class CompanyAdminInvitationService {

    private static final String EVENT_TYPE =
            "company.admin.invitation.requested";

    private static final String ALLOWED_ROLE =
            "COMPANY_ADMIN";

    private final TenantRepository tenantRepository;

    private final CompanyAdminInvitationRepository invitationRepository;

    private final CompanyAdminEmailService emailService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${omnitruth.invitation.base-url}")
    private String invitationBaseUrl;

    @Value("${omnitruth.invitation.expiry-hours:48}")
    private long expiryHours;

    public CompanyAdminInvitationService(
            TenantRepository tenantRepository,
            CompanyAdminInvitationRepository invitationRepository,
            CompanyAdminEmailService emailService) {

        this.tenantRepository = tenantRepository;
        this.invitationRepository = invitationRepository;
        this.emailService = emailService;
    }

    /**
     * Processes a Company Admin invitation request.
     *
     * The tenant must already exist.
     * This method does not create a tenant.
     */
    @Transactional
    public void processInvitation(
            CompanyAdminInvitationRequestedEvent event) {

        // 1. Validate the incoming event.
        validateEvent(event);

        // 2. Normalize the email address.
        String normalizedEmail = event.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        // 3. Idempotency check.
        if (invitationRepository.existsByEventId(event.eventId())) {
            return;
        }

        // 4. Verify that the tenant exists and its code matches.
        Tenant tenant = tenantRepository
                .findByTenantIdAndTenantCode(
                        event.tenantId(),
                        event.tenantCode()
                )
                .orElseThrow(() -> new IllegalStateException(
                        "Tenant not found or tenant code mismatch"
                ));

        // 5. Generate a secure invitation token.
        String rawToken = generateSecureToken();

        // 6. Store only the token hash.
        String tokenHash = hashToken(rawToken);

        // 7. Calculate invitation expiry.
        Instant now = Instant.now();

        Instant expiresAt = now.plus(
                expiryHours,
                ChronoUnit.HOURS
        );

        // 8. Create the invitation record.
        CompanyAdminInvitation invitation =
                new CompanyAdminInvitation();

        invitation.setInvitationId(UUID.randomUUID());

        invitation.setEventId(event.eventId());

        invitation.setTenantId(tenant.getTenantId());

        invitation.setEmail(normalizedEmail);

        invitation.setRequestedRole(ALLOWED_ROLE);

        invitation.setTokenHash(tokenHash);

        invitation.setStatus("PENDING");

        invitation.setExpiresAt(expiresAt);

        invitation.setCreatedAt(now);

        // 9. Persist the invitation.
        invitationRepository.save(invitation);

        // 10. Build the invitation URL.
        String invitationLink =
                invitationBaseUrl
                        + "?token="
                        + URLEncoder.encode(
                        rawToken,
                        StandardCharsets.UTF_8
                );

        // 11. Send the invitation email.
        emailService.sendInvitation(
                normalizedEmail,
                tenant.getTenantCode(),
                invitationLink,
                expiresAt
        );
    }

    /**
     * Validates the Kafka invitation event.
     */
    private void validateEvent(
            CompanyAdminInvitationRequestedEvent event) {

        if (event == null) {
            throw new IllegalArgumentException("Invitation event cannot be null");
        }

        if (!EVENT_TYPE.equals(event.eventType())) {
            throw new IllegalArgumentException(
                    "Unsupported event type: " + event.eventType()
            );
        }

        if (event.eventVersion() != 1) {
            throw new IllegalArgumentException(
                    "Unsupported event version: " + event.eventVersion()
            );
        }

        if (!"COMPANY_ADMIN".equals(event.requestedRole())) {
            throw new IllegalArgumentException(
                    "Unsupported invitation role: " + event.requestedRole()
            );
        }
    }

    /**
     * Generates a cryptographically secure random token.
     */
    private String generateSecureToken() {

        byte[] bytes = new byte[32];

        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    /**
     * Hashes the invitation token using SHA-256.
     *
     * The raw token is never stored in the database.
     */
    private String hashToken(String token) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Unable to hash invitation token",
                    exception
            );
        }
    }
}
