package com.omnitruth.tenant.management.service;

import com.omnitruth.tenant.management.domain.*;
import com.omnitruth.tenant.management.repository.*;
import com.omnitruth.tenant.management.web.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import com.omnitruth.tenant.management.domain.Tenant;
import com.omnitruth.tenant.management.repository.TenantRepository;
import org.springframework.security.oauth2.jwt.Jwt;

@Service
@Transactional
public class TenantWorkspaceService {
    private final TenantContextService tenantContext;
    private final TenantClientRepository clients;
    private final TenantApiEndpointRepository endpoints;
    private final TenantToolDefinitionRepository tools;
    private final TenantRepository tenants;
    public TenantWorkspaceService(
            TenantContextService tenantContext,
            TenantClientRepository clients,
            TenantApiEndpointRepository endpoints,
            TenantToolDefinitionRepository tools,
            TenantRepository tenants) {

        this.tenantContext = tenantContext;
        this.clients = clients;
        this.endpoints = endpoints;
        this.tools = tools;
        this.tenants = tenants;
    }

    @Transactional(readOnly = true)
    public TenantDashboardResponse dashboard(org.springframework.security.oauth2.jwt.Jwt jwt) {
        Tenant tenant = tenantContext.requireTenant(jwt);
        long clientCount = clients.countByTenantId(tenant.getTenantId());
        long endpointCount = endpoints.countByTenantId(tenant.getTenantId());
        long toolCount = tools.countByTenantId(tenant.getTenantId());
        long activeClients = clients.findAllByTenantIdOrderByCreatedAtDesc(tenant.getTenantId())
                .stream().filter(c -> "ACTIVE".equals(c.getStatus())).count();
        long enabledEndpoints = endpoints.findAllByTenantIdOrderByCreatedAtDesc(tenant.getTenantId())
                .stream().filter(TenantApiEndpoint::isEnabled).count();
        long enabledTools = tools.findAllByTenantIdOrderByCreatedAtDesc(tenant.getTenantId())
                .stream().filter(TenantToolDefinition::isEnabled).count();
        return new TenantDashboardResponse(
                new TenantDashboardResponse.TenantSummary(tenant.getTenantId(), tenant.getTenantCode(),
                        tenant.getCompanyName(), tenant.getStatus()),
                clientCount, activeClients, endpointCount, enabledEndpoints, toolCount, enabledTools);
    }

    @Transactional(readOnly = true)
    public List<TenantClientResponse> listClients(org.springframework.security.oauth2.jwt.Jwt jwt) {
        UUID tenantId = tenantContext.requireTenant(jwt).getTenantId();
        return clients.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(this::toResponse).toList();
    }

    public TenantClientResponse createClient(org.springframework.security.oauth2.jwt.Jwt jwt, TenantClientRequest request) {
        UUID tenantId = tenantContext.requireTenant(jwt).getTenantId();
        TenantClient entity = new TenantClient();
        entity.setClientId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setCreatedAt(Instant.now());
        applyClient(entity, request);
        return toResponse(clients.save(entity));
    }

    public TenantClientResponse updateClient(org.springframework.security.oauth2.jwt.Jwt jwt, UUID id, TenantClientRequest request) {
        UUID tenantId = tenantContext.requireTenant(jwt).getTenantId();
        TenantClient entity = clients.findByClientIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found."));
        applyClient(entity, request);
        return toResponse(clients.save(entity));
    }

    private void applyClient(TenantClient entity, TenantClientRequest request) {
        String status = request.status().trim().toUpperCase(Locale.ROOT);
        if (!List.of("ACTIVE", "INACTIVE").contains(status)) {
            throw new IllegalArgumentException("Client status must be ACTIVE or INACTIVE.");
        }
        entity.setClientName(request.clientName().trim());
        entity.setContactEmail(blankToNull(request.contactEmail()));
        entity.setExternalReference(blankToNull(request.externalReference()));
        entity.setNotes(blankToNull(request.notes()));
        entity.setStatus(status);
        entity.setUpdatedAt(Instant.now());
    }

    @Transactional(readOnly = true)
    public List<TenantApiEndpointResponse> listEndpoints(org.springframework.security.oauth2.jwt.Jwt jwt) {
        UUID tenantId = tenantContext.requireTenant(jwt).getTenantId();
        return endpoints.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(this::toResponse).toList();
    }

    public TenantApiEndpointResponse createEndpoint(org.springframework.security.oauth2.jwt.Jwt jwt, TenantApiEndpointRequest request) {
        UUID tenantId = tenantContext.requireTenant(jwt).getTenantId();
        TenantApiEndpoint entity = new TenantApiEndpoint();
        entity.setEndpointId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setCreatedAt(Instant.now());
        applyEndpoint(entity, request);
        return toResponse(endpoints.save(entity));
    }

    public TenantApiEndpointResponse updateEndpoint(org.springframework.security.oauth2.jwt.Jwt jwt, UUID id, TenantApiEndpointRequest request) {
        UUID tenantId = tenantContext.requireTenant(jwt).getTenantId();
        TenantApiEndpoint entity = endpoints.findByEndpointIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("API endpoint not found."));
        applyEndpoint(entity, request);
        return toResponse(endpoints.save(entity));
    }

    private void applyEndpoint(TenantApiEndpoint entity, TenantApiEndpointRequest request) {
        String method = request.httpMethod().trim().toUpperCase(Locale.ROOT);
        String auth = request.authType().trim().toUpperCase(Locale.ROOT);
        if (!List.of("GET", "POST", "PUT", "PATCH", "DELETE").contains(method)) {
            throw new IllegalArgumentException("Unsupported HTTP method.");
        }
        if (!List.of("NONE", "BEARER_SECRET_REFERENCE", "API_KEY_SECRET_REFERENCE", "OAUTH2_CLIENT_CREDENTIALS_REFERENCE").contains(auth)) {
            throw new IllegalArgumentException("Unsupported authentication type.");
        }
        URI uri;
        try { uri = URI.create(request.baseUrl().trim()); }
        catch (RuntimeException ex) { throw new IllegalArgumentException("API endpoint URL is invalid."); }
        boolean localHttp = "http".equalsIgnoreCase(uri.getScheme()) &&
                ("localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost()));
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !localHttp) {
            throw new IllegalArgumentException("API endpoint URLs must use HTTPS (HTTP is allowed only for localhost development).");
        }
        if (uri.getHost() == null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("API endpoint URL must have a host and must not contain credentials.");
        }
        if (!"NONE".equals(auth) && (request.secretReference() == null || request.secretReference().isBlank())) {
            throw new IllegalArgumentException("Provide a Key Vault secret reference for the selected authentication type.");
        }
        entity.setEndpointName(request.endpointName().trim());
        entity.setBaseUrl(uri.toString());
        entity.setHttpMethod(method);
        entity.setAuthType(auth);
        entity.setSecretReference(blankToNull(request.secretReference()));
        entity.setDescription(blankToNull(request.description()));
        entity.setEnabled(request.enabled());
        entity.setUpdatedAt(Instant.now());
    }

    @Transactional(readOnly = true)
    public List<TenantToolResponse> listTools(org.springframework.security.oauth2.jwt.Jwt jwt) {
        UUID tenantId = tenantContext.requireTenant(jwt).getTenantId();
        return tools.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(this::toResponse).toList();
    }

    public TenantToolResponse createTool(org.springframework.security.oauth2.jwt.Jwt jwt, TenantToolRequest request) {
        UUID tenantId = tenantContext.requireTenant(jwt).getTenantId();
        requireEndpoint(tenantId, request.endpointId());
        TenantToolDefinition entity = new TenantToolDefinition();
        entity.setToolId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setCreatedAt(Instant.now());
        applyTool(entity, request);
        return toResponse(tools.save(entity));
    }

    public TenantToolResponse updateTool(org.springframework.security.oauth2.jwt.Jwt jwt, UUID id, TenantToolRequest request) {
        UUID tenantId = tenantContext.requireTenant(jwt).getTenantId();
        requireEndpoint(tenantId, request.endpointId());
        TenantToolDefinition entity = tools.findByToolIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tool definition not found."));
        applyTool(entity, request);
        return toResponse(tools.save(entity));
    }

    private void requireEndpoint(UUID tenantId, UUID endpointId) {
        TenantApiEndpoint endpoint = endpoints.findByEndpointIdAndTenantId(endpointId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Selected API endpoint does not belong to this tenant."));
        if (!endpoint.isEnabled()) throw new IllegalArgumentException("Selected API endpoint is disabled.");
    }

    private void applyTool(TenantToolDefinition entity, TenantToolRequest request) {
        String schema = request.inputSchema().trim();
        if (!schema.startsWith("{") || !schema.endsWith("}")) {
            throw new IllegalArgumentException("Tool input schema must be a JSON object.");
        }
        entity.setToolName(request.toolName().trim());
        entity.setDescription(request.description().trim());
        entity.setEndpointId(request.endpointId());
        entity.setInputSchema(schema);
        entity.setEnabled(request.enabled());
        entity.setUpdatedAt(Instant.now());
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private TenantClientResponse toResponse(TenantClient c) {
        return new TenantClientResponse(c.getClientId(), c.getClientName(), c.getContactEmail(),
                c.getExternalReference(), c.getNotes(), c.getStatus(), c.getCreatedAt(), c.getUpdatedAt());
    }
    private TenantApiEndpointResponse toResponse(TenantApiEndpoint e) {
        return new TenantApiEndpointResponse(e.getEndpointId(), e.getEndpointName(), e.getBaseUrl(),
                e.getHttpMethod(), e.getAuthType(), e.getSecretReference(), e.getDescription(),
                e.isEnabled(), e.getCreatedAt(), e.getUpdatedAt());
    }
    private TenantToolResponse toResponse(TenantToolDefinition t) {
        return new TenantToolResponse(t.getToolId(), t.getEndpointId(), t.getToolName(), t.getDescription(),
                t.getInputSchema(), t.isEnabled(), t.getCreatedAt(), t.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public TenantProfileResponse getTenantProfile(Jwt jwt) {

        Tenant tenant = tenantContext.requireTenant(jwt);

        return toProfileResponse(tenant);
    }

    public TenantProfileResponse updateTenantProfile(
            Jwt jwt,
            TenantProfileRequest request) {

        // Identify tenant from authenticated Entra JWT
        Tenant tenant = tenantContext.requireTenant(jwt);

        // Update only fields permitted for Company Admin
        tenant.setCompanyName(request.companyName().trim());

        tenant.setIndustry(
                request.industry() == null || request.industry().isBlank()
                        ? null
                        : request.industry().trim()
        );

        tenant.setUpdatedAt(Instant.now());

        Tenant updatedTenant = tenants.save(tenant);

        return toProfileResponse(updatedTenant);
    }

    private TenantProfileResponse toProfileResponse(Tenant tenant) {

        return new TenantProfileResponse(
                tenant.getTenantId(),
                tenant.getTenantCode(),
                tenant.getCompanyName(),
                tenant.getIndustry(),
                tenant.getStatus(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt()
        );
    }

}
