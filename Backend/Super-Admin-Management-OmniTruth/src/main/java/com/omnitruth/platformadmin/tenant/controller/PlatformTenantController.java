
package com.omnitruth.platformadmin.tenant.controller;

import java.net.URI;
import java.util.UUID;

import com.omnitruth.platformadmin.tenant.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
@CrossOrigin(origins = {"http://localhost:4200", "https://platform.omnitruth.com"}
)
public class PlatformTenantController {

    private static final String TENANT_RESOURCE_PATH = "/api/platform/v1/tenants/";
    private final TenantService tenantService;
    private final PlatformOnboardingService onboardingService;

    public PlatformTenantController(TenantService tenantService, PlatformOnboardingService onboardingService) {
        this.tenantService = tenantService;
        this.onboardingService = onboardingService;
    }

    @Operation(summary = "Create a new tenant",description = "Creates a new tenant in the system. The request body must contain the necessary information for tenant creation.")
    @ApiResponse(responseCode = "201", description = "Tenant created successfully")
    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request, @AuthenticationPrincipal Jwt jwt) {

        TenantResponse created = tenantService.create(request, jwt.getSubject());
        URI location = URI.create(TENANT_RESOURCE_PATH + created.tenantId());
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Get tenant details",description = "Retrieves the details of a all tenants in pagable manner.")
    @ApiResponse(responseCode = "200", description = "Tenant details retrieved successfully")
    @GetMapping
    public ResponseEntity<Page<TenantResponse>> listTenants(@RequestParam(required = false) TenantStatus status, Pageable pageable) {

        Page<TenantResponse> tenants = tenantService.list(status, pageable);
        return ResponseEntity.ok(tenants);
    }

    @Operation(summary = "Get tenant details by ID", description = "Retrieves the details of a specific tenant by its ID.")
    @ApiResponse(responseCode = "200", description = "Tenant details retrieved successfully")
    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable UUID tenantId) {
        TenantResponse tenant = tenantService.get(tenantId);

        return ResponseEntity.ok(tenant);
    }

@Operation(summary = "Update tenant details", description = "Updates the details of a specific tenant by its ID. The request body must contain the updated information.")
@ApiResponse(responseCode = "200", description = "Tenant details updated successfully")
    @PatchMapping("/{tenantId}/status")
    public ResponseEntity<TenantResponse> changeStatus(@PathVariable UUID tenantId, @Valid @RequestBody UpdateTenantStatusRequest request, @AuthenticationPrincipal Jwt jwt) {

        TenantResponse updated = tenantService.changeStatus(tenantId, request.status(), jwt.getSubject());
        return ResponseEntity.ok(updated);
    }
    @Operation(description = "Invite first tenant admin",summary = "Invite first tenant admin by adding the email. The tenant should be entra id setup")
    @ApiResponse(responseCode = "200", description = "Tenant admin invited successfully")
    @PostMapping("/{tenantId}/company-admin-invitations")
    public ResponseEntity<InvitationAcceptedResponse> inviteFirstCompanyAdmin(
            @PathVariable UUID tenantId,
            @Valid @RequestBody InviteCompanyAdminRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        InvitationAcceptedResponse response = onboardingService.inviteFirstCompanyAdmin(
                        tenantId,
                        request.email(),
                        jwt.getSubject()
                );

        return ResponseEntity.accepted().body(response);
    }

}
