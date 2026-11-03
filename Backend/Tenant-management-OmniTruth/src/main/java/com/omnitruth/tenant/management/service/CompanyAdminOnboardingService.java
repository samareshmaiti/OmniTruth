package com.omnitruth.tenant.management.service;

import com.omnitruth.tenant.management.config.InvitationTokenService;
import com.omnitruth.tenant.management.domain.CompanyAdminInvitation;
import com.omnitruth.tenant.management.domain.Tenant;
import com.omnitruth.tenant.management.domain.TenantIdentityMapping;
import com.omnitruth.tenant.management.domain.TenantUser;
import com.omnitruth.tenant.management.repository.CompanyAdminInvitationRepository;
import com.omnitruth.tenant.management.repository.TenantIdentityMappingRepository;
import com.omnitruth.tenant.management.repository.TenantRepository;
import com.omnitruth.tenant.management.repository.TenantUserRepository;
import com.omnitruth.tenant.management.web.dto.onboarding.InvitationDetailsResponse;
import com.omnitruth.tenant.management.web.dto.onboarding.OnboardingCompleteResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class CompanyAdminOnboardingService {

    private static final String ROLE_COMPANY_ADMIN = "COMPANY_ADMIN";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACCEPTED_PENDING_SSO = "ACCEPTED_PENDING_SSO";
    private static final String STATUS_ACCEPTED = "ACCEPTED";

    private final CompanyAdminInvitationRepository invitations;
    private final TenantRepository tenants;
    private final TenantIdentityMappingRepository mappings;
    private final TenantUserRepository users;
    private final InvitationTokenService tokenService;

    public CompanyAdminOnboardingService(
            CompanyAdminInvitationRepository invitations,
            TenantRepository tenants,
            TenantIdentityMappingRepository mappings,
            TenantUserRepository users,
            InvitationTokenService tokenService) {
        this.invitations = invitations;
        this.tenants = tenants;
        this.mappings = mappings;
        this.users = users;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public InvitationDetailsResponse validateInvitation(String rawToken) {
        CompanyAdminInvitation invitation = findInvitation(rawToken, false);
        ensureRoleSupported(invitation);
        if (!STATUS_ACCEPTED.equals(invitation.getStatus())) {
            ensureNotExpired(invitation);
            ensureStatus(invitation, STATUS_PENDING, STATUS_ACCEPTED_PENDING_SSO);
        }
        return toDetails(invitation);
    }

    @Transactional
    public InvitationDetailsResponse acceptInvitation(String rawToken) {
        CompanyAdminInvitation invitation = findInvitation(rawToken, true);
        ensureRoleSupported(invitation);

        if (STATUS_ACCEPTED_PENDING_SSO.equals(invitation.getStatus())
                || STATUS_ACCEPTED.equals(invitation.getStatus())) {
            return toDetails(invitation);
        }
        ensureNotExpired(invitation);
        ensureStatus(invitation, STATUS_PENDING);

        invitation.setStatus(STATUS_ACCEPTED_PENDING_SSO);
        invitation.setAcceptedAt(Instant.now());
        return toDetails(invitations.save(invitation));
    }

    @Transactional
    public OnboardingCompleteResponse completeOnboarding(String rawToken, Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Microsoft sign-in is required.");
        }

        CompanyAdminInvitation invitation = findInvitation(rawToken, true);
        ensureRoleSupported(invitation);

        String entraTenantId = normalizedGuidClaim(jwt, "tid");
        String entraObjectId = normalizedGuidClaim(jwt, "oid");
        UUID internalTenantId = invitation.getTenantId();

        // A completed invitation is idempotent only for the same Entra identity.
        // It remains idempotent even after the original invitation expiry time.
        if (STATUS_ACCEPTED.equals(invitation.getStatus())) {
            TenantUser existing = users.findByEntraTenantIdIgnoreCaseAndEntraObjectIdIgnoreCase(
                    entraTenantId, entraObjectId)
                    .orElseThrow(() -> conflict("This invitation has already been completed by another identity."));
            boolean sameActiveMapping = mappings.findByTenantIdAndActiveTrue(internalTenantId)
                    .filter(mapping -> mapping.getEntraTenantId().equalsIgnoreCase(entraTenantId))
                    .isPresent();
            if (!internalTenantId.equals(existing.getTenantId())
                    || !ROLE_COMPANY_ADMIN.equals(existing.getAppRole())
                    || !"ACTIVE".equals(existing.getStatus())
                    || !sameActiveMapping) {
                throw conflict("This invitation has already been completed by another identity.");
            }
            return completedResponse(internalTenantId, entraTenantId);
        }

        ensureNotExpired(invitation);
        ensureStatus(invitation, STATUS_ACCEPTED_PENDING_SSO);
        Tenant tenant = tenants.findById(internalTenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invited organization no longer exists."));

        ensureTenantIdentityMapping(internalTenantId, entraTenantId);
        provisionCompanyAdmin(tenant, entraTenantId, entraObjectId, invitation.getEmail());

        invitation.setStatus(STATUS_ACCEPTED);
        if (invitation.getAcceptedAt() == null) {
            invitation.setAcceptedAt(Instant.now());
        }
        invitations.save(invitation);

        return new OnboardingCompleteResponse(
                tenant.getTenantId(),
                tenant.getTenantCode(),
                tenant.getCompanyName(),
                entraTenantId,
                STATUS_ACCEPTED,
                "Company Admin onboarding completed. You can now open your OmniTruth dashboard."
        );
    }

    private CompanyAdminInvitation findInvitation(String rawToken, boolean lock) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 200) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found or invalid.");
        }
        String hash = tokenService.hashToken(rawToken.trim());
        return (lock ? invitations.findByTokenHashForUpdate(hash) : invitations.findByTokenHash(hash))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invitation not found or invalid."));
    }

    private void ensureNotExpired(CompanyAdminInvitation invitation) {
        if (invitation.getExpiresAt() == null || !invitation.getExpiresAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "This invitation has expired.");
        }
    }

    private void ensureRoleSupported(CompanyAdminInvitation invitation) {
        if (!ROLE_COMPANY_ADMIN.equals(invitation.getRequestedRole())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This invitation role is not supported.");
        }
    }

    private void ensureStatus(CompanyAdminInvitation invitation, String... allowed) {
        for (String candidate : allowed) {
            if (candidate.equals(invitation.getStatus())) {
                return;
            }
        }
        throw conflict("This invitation is not available in its current state.");
    }

    private void ensureTenantIdentityMapping(UUID tenantId, String entraTenantId) {
        mappings.findByTenantId(tenantId).ifPresent(existing -> {
            if (!existing.isActive()) {
                throw conflict("The organization's existing Microsoft identity mapping is inactive.");
            }
            if (!existing.getEntraTenantId().equalsIgnoreCase(entraTenantId)) {
                throw conflict("This organization is already linked to a different Microsoft Entra tenant.");
            }
        });

        mappings.findByEntraTenantIdIgnoreCase(entraTenantId).ifPresent(existing -> {
            if (!existing.isActive() || !tenantId.equals(existing.getTenantId())) {
                throw conflict("This Microsoft Entra tenant is already linked to another OmniTruth organization.");
            }
        });

        if (mappings.findByTenantId(tenantId).isEmpty()) {
            TenantIdentityMapping mapping = new TenantIdentityMapping();
            mapping.setMappingId(UUID.randomUUID());
            mapping.setTenantId(tenantId);
            mapping.setEntraTenantId(entraTenantId);
            mapping.setActive(true);
            mapping.setCreatedAt(Instant.now());
            mappings.save(mapping);
        }
    }

    private void provisionCompanyAdmin(
            Tenant tenant, String entraTenantId, String entraObjectId, String invitationEmail) {

        TenantUser user = users.findByEntraTenantIdIgnoreCaseAndEntraObjectIdIgnoreCase(
                        entraTenantId, entraObjectId)
                .orElseGet(() -> {
                    TenantUser created = new TenantUser();
                    created.setUserId(UUID.randomUUID());
                    created.setTenantId(tenant.getTenantId());
                    created.setEntraTenantId(entraTenantId);
                    created.setEntraObjectId(entraObjectId);
                    created.setCreatedAt(Instant.now());
                    return created;
                });

        if (!tenant.getTenantId().equals(user.getTenantId())) {
            throw conflict("This Microsoft identity is already registered with another OmniTruth organization.");
        }

        user.setInvitationEmail(invitationEmail);
        user.setAppRole(ROLE_COMPANY_ADMIN);
        user.setStatus("ACTIVE");
        user.setUpdatedAt(Instant.now());
        users.save(user);
    }

    private String normalizedGuidClaim(Jwt jwt, String claimName) {
        String value = jwt.getClaimAsString(claimName);
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Microsoft token is missing a required identity claim.");
        }
        try {
            return UUID.fromString(value).toString().toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Microsoft token contains an invalid identity claim.");
        }
    }

    private InvitationDetailsResponse toDetails(CompanyAdminInvitation invitation) {
        Tenant tenant = tenants.findById(invitation.getTenantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invited organization no longer exists."));
        return new InvitationDetailsResponse(
                invitation.getInvitationId(),
                tenant.getCompanyName(),
                tenant.getTenantCode(),
                invitation.getEmail(),
                invitation.getRequestedRole(),
                invitation.getStatus(),
                invitation.getExpiresAt()
        );
    }

    private OnboardingCompleteResponse completedResponse(UUID tenantId, String entraTenantId) {
        Tenant tenant = tenants.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found."));
        return new OnboardingCompleteResponse(
                tenant.getTenantId(), tenant.getTenantCode(), tenant.getCompanyName(),
                entraTenantId, STATUS_ACCEPTED,
                "Company Admin onboarding was already completed for this Microsoft identity."
        );
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
