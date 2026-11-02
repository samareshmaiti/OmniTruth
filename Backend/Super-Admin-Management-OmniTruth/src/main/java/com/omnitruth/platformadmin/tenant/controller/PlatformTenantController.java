
package com.omnitruth.platformadmin.tenant.controller;

import java.net.URI;
import java.util.UUID;

import com.omnitruth.platformadmin.tenant.dto.*;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.omnitruth.platformadmin.tenant.domain.TenantStatus;
import com.omnitruth.platformadmin.tenant.service.PlatformOnboardingService;
import com.omnitruth.platformadmin.tenant.service.TenantService;

/**
 * REST controller for platform-level tenant administration.
 *
 * All endpoints require an authenticated platform super administrator.
 */
@RestController
@RequestMapping("/api/platform/v1/tenants")
@PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
@CrossOrigin(
        origins = {
                "http://localhost:4200",
                "https://platform.omnitruth.com"
        }
)
public class PlatformTenantController {

    private static final String TENANT_RESOURCE_PATH =
            "/api/platform/v1/tenants/";

    private final TenantService tenantService;
    private final PlatformOnboardingService onboardingService;

    public PlatformTenantController(
            TenantService tenantService,
            PlatformOnboardingService onboardingService) {

        this.tenantService = tenantService;
        this.onboardingService = onboardingService;
    }

    /**
     * Creates a new tenant.
     */
    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(
            @Valid @RequestBody CreateTenantRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        TenantResponse created =
                tenantService.create(request, jwt.getSubject());

        URI location = URI.create(
                TENANT_RESOURCE_PATH + created.tenantId()
        );

        return ResponseEntity
                .created(location)
                .body(created);
    }

    /**
     * Returns a paginated list of tenants.
     * Optionally filters tenants by status.
     */
    @GetMapping
    public ResponseEntity<Page<TenantResponse>> listTenants(
            @RequestParam(required = false) TenantStatus status,
            Pageable pageable) {

        Page<TenantResponse> tenants =
                tenantService.list(status, pageable);

        return ResponseEntity.ok(tenants);
    }

    /**
     * Returns a tenant by its unique identifier.
     */
    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> getTenant(
            @PathVariable UUID tenantId) {

        TenantResponse tenant =
                tenantService.get(tenantId);

        return ResponseEntity.ok(tenant);
    }

    /**
     * Updates the status of an existing tenant.
     */
    @PatchMapping("/{tenantId}/status")
    public ResponseEntity<TenantResponse> changeStatus(
            @PathVariable UUID tenantId,
            @Valid @RequestBody UpdateTenantStatusRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        TenantResponse updated =
                tenantService.changeStatus(
                        tenantId,
                        request.status(),
                        jwt.getSubject()
                );

        return ResponseEntity.ok(updated);
    }

    /**
     * Invites the first company administrator for a tenant.
     */

    @PostMapping("/{tenantId}/company-admin-invitations")
    public ResponseEntity<InvitationAcceptedResponse> inviteFirstCompanyAdmin(
            @PathVariable UUID tenantId,
            @Valid @RequestBody InviteCompanyAdminRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        InvitationAcceptedResponse response =
                onboardingService.inviteFirstCompanyAdmin(
                        tenantId,
                        request.email(),
                        jwt.getSubject()
                );

        return ResponseEntity.accepted().body(response);
    }

}
