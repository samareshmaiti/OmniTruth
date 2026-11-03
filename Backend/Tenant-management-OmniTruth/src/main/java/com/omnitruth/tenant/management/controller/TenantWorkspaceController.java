package com.omnitruth.tenant.management.controller;

import com.omnitruth.tenant.management.domain.TenantProfileRequest;
import com.omnitruth.tenant.management.domain.TenantProfileResponse;
import com.omnitruth.tenant.management.service.TenantWorkspaceService;
import com.omnitruth.tenant.management.web.dto.*;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenant/v1")
@PreAuthorize("hasRole('COMPANY_ADMIN')")
public class TenantWorkspaceController {

    private final TenantWorkspaceService service;

    public TenantWorkspaceController(TenantWorkspaceService service) {
        this.service = service;
    }

    // ==========================================
    // TENANT DASHBOARD
    // ==========================================

    @GetMapping("/dashboard")
    public TenantDashboardResponse dashboard(
            @AuthenticationPrincipal Jwt jwt) {

        return service.dashboard(jwt);
    }

    // ==========================================
    // CLIENT MANAGEMENT
    // ==========================================

    @GetMapping("/clients")
    public List<TenantClientResponse> getClients(
            @AuthenticationPrincipal Jwt jwt) {

        return service.listClients(jwt);
    }

    @PostMapping("/clients")
    @ResponseStatus(HttpStatus.CREATED)
    public TenantClientResponse createClient(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TenantClientRequest request) {

        return service.createClient(jwt, request);
    }

    @PutMapping("/clients/{clientId}")
    public TenantClientResponse updateClient(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID clientId,
            @Valid @RequestBody TenantClientRequest request) {

        return service.updateClient(jwt, clientId, request);
    }

    // ==========================================
    // API ENDPOINT MANAGEMENT
    // ==========================================

    @GetMapping("/api-endpoints")
    public List<TenantApiEndpointResponse> getApiEndpoints(
            @AuthenticationPrincipal Jwt jwt) {

        return service.listEndpoints(jwt);
    }

    @PostMapping("/api-endpoints")
    @ResponseStatus(HttpStatus.CREATED)
    public TenantApiEndpointResponse createApiEndpoint(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TenantApiEndpointRequest request) {

        return service.createEndpoint(jwt, request);
    }

    @PutMapping("/api-endpoints/{endpointId}")
    public TenantApiEndpointResponse updateApiEndpoint(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID endpointId,
            @Valid @RequestBody TenantApiEndpointRequest request) {

        return service.updateEndpoint(jwt, endpointId, request);
    }

    // ==========================================
    // AI TOOL CALLING MANAGEMENT
    // ==========================================

    @GetMapping("/tools")
    public List<TenantToolResponse> getTools(
            @AuthenticationPrincipal Jwt jwt) {

        return service.listTools(jwt);
    }

    @PostMapping("/tools")
    @ResponseStatus(HttpStatus.CREATED)
    public TenantToolResponse createTool(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TenantToolRequest request) {

        return service.createTool(jwt, request);
    }

    @PutMapping("/tools/{toolId}")
    public TenantToolResponse updateTool(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID toolId,
            @Valid @RequestBody TenantToolRequest request) {

        return service.updateTool(jwt, toolId, request);
    }

// ==========================================
// COMPANY PROFILE
// ==========================================

    @GetMapping("/profile")
    public TenantProfileResponse getTenantProfile(
            @AuthenticationPrincipal Jwt jwt) {

        return service.getTenantProfile(jwt);
    }

    @PutMapping("/profile")
    public TenantProfileResponse updateTenantProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TenantProfileRequest request) {

        return service.updateTenantProfile(jwt, request);
    }

}
