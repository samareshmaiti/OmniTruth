package com.omnitruth.tenant.management.web;

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

// Legacy duplicate of controller.TenantWorkspaceController. That controller exposes
// the same dashboard/client/endpoint/tool routes plus the profile routes. Keep this
// source for compatibility, but do not register a second set of identical Spring mappings.
@Deprecated(forRemoval = false)
public class TenantWorkspaceController {
    private final TenantWorkspaceService service;
    public TenantWorkspaceController(TenantWorkspaceService service) { this.service = service; }

    @GetMapping("/dashboard")
    public TenantDashboardResponse dashboard(@AuthenticationPrincipal Jwt jwt) {
        return service.dashboard(jwt);
    }

    @GetMapping("/clients")
    public List<TenantClientResponse> clients(@AuthenticationPrincipal Jwt jwt) {
        return service.listClients(jwt);
    }

    @PostMapping("/clients")
    @ResponseStatus(HttpStatus.CREATED)
    public TenantClientResponse createClient(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TenantClientRequest request) {
        return service.createClient(jwt, request);
    }

    @PutMapping("/clients/{clientId}")
    public TenantClientResponse updateClient(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID clientId, @Valid @RequestBody TenantClientRequest request) {
        return service.updateClient(jwt, clientId, request);
    }

    @GetMapping("/api-endpoints")
    public List<TenantApiEndpointResponse> endpoints(@AuthenticationPrincipal Jwt jwt) {
        return service.listEndpoints(jwt);
    }

    @PostMapping("/api-endpoints")
    @ResponseStatus(HttpStatus.CREATED)
    public TenantApiEndpointResponse createEndpoint(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TenantApiEndpointRequest request) {
        return service.createEndpoint(jwt, request);
    }

    @PutMapping("/api-endpoints/{endpointId}")
    public TenantApiEndpointResponse updateEndpoint(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID endpointId, @Valid @RequestBody TenantApiEndpointRequest request) {
        return service.updateEndpoint(jwt, endpointId, request);
    }

    @GetMapping("/tools")
    public List<TenantToolResponse> tools(@AuthenticationPrincipal Jwt jwt) {
        return service.listTools(jwt);
    }

    @PostMapping("/tools")
    @ResponseStatus(HttpStatus.CREATED)
    public TenantToolResponse createTool(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TenantToolRequest request) {
        return service.createTool(jwt, request);
    }

    @PutMapping("/tools/{toolId}")
    public TenantToolResponse updateTool(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID toolId, @Valid @RequestBody TenantToolRequest request) {
        return service.updateTool(jwt, toolId, request);
    }
}
