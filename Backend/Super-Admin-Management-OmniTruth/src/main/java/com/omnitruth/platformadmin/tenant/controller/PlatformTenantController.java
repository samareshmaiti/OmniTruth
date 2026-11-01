package com.omnitruth.platformadmin.tenant.controller;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.omnitruth.platformadmin.tenant.domain.TenantStatus;
import com.omnitruth.platformadmin.tenant.dto.CompanyAdminInvitationResponse;
import com.omnitruth.platformadmin.tenant.dto.CreateTenantRequest;
import com.omnitruth.platformadmin.tenant.dto.InviteCompanyAdminRequest;
import com.omnitruth.platformadmin.tenant.dto.TenantResponse;
import com.omnitruth.platformadmin.tenant.dto.UpdateTenantStatusRequest;
import com.omnitruth.platformadmin.tenant.service.PlatformOnboardingService;
import com.omnitruth.platformadmin.tenant.service.TenantService;

@RestController
@RequestMapping("/api/platform/v1/tenants")
@PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
@CrossOrigin(value = {"http://localhost:4200", "https://platform.omnitruth.com"})
public class PlatformTenantController {
    private final TenantService tenantService;
    private final PlatformOnboardingService onboardingService;

    public PlatformTenantController(TenantService tenantService, PlatformOnboardingService onboardingService) {
        this.tenantService = tenantService;
        this.onboardingService = onboardingService;
    }

    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        TenantResponse created = tenantService.create(request, jwt.getSubject());
        return ResponseEntity.created(URI.create("/api/platform/v1/tenants/" + created.tenantId())).body(created);
    }

    @GetMapping
    public Page<TenantResponse> listTenants(@RequestParam(required = false) TenantStatus status, Pageable pageable) {
        return tenantService.list(status, pageable);
    }

    @GetMapping("/{tenantId}")
    public TenantResponse getTenant(@PathVariable UUID tenantId) {
        return tenantService.get(tenantId);
    }

    @PatchMapping("/{tenantId}/status")
    public TenantResponse changeStatus(@PathVariable UUID tenantId,
            @Valid @RequestBody UpdateTenantStatusRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return tenantService.changeStatus(tenantId, request.status(), jwt.getSubject());
    }

    @PostMapping("/{tenantId}/company-admin-invitations")
    public CompanyAdminInvitationResponse inviteFirstCompanyAdmin(@PathVariable UUID tenantId,
            @Valid @RequestBody InviteCompanyAdminRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return onboardingService.inviteFirstCompanyAdmin(tenantId, request.email(), jwt.getSubject());
    }
}
