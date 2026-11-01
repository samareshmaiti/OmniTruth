package com.omnitruth.platformadmin.tenant.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.omnitruth.platformadmin.audit.client.AuditManagementClient;
import com.omnitruth.platformadmin.common.controller.ConflictException;
import com.omnitruth.platformadmin.common.controller.ResourceNotFoundException;
import com.omnitruth.platformadmin.tenant.domain.Tenant;
import com.omnitruth.platformadmin.tenant.domain.TenantStatus;
import com.omnitruth.platformadmin.tenant.dto.CreateTenantRequest;
import com.omnitruth.platformadmin.tenant.dto.TenantResponse;
import com.omnitruth.platformadmin.tenant.repository.TenantRepository;

@Service
public class TenantService {
    private final TenantRepository tenantRepository;
    private final AuditManagementClient auditClient;

    public TenantService(TenantRepository tenantRepository, AuditManagementClient auditClient) {
        this.tenantRepository = tenantRepository;
        this.auditClient = auditClient;
    }

    @Transactional
    public TenantResponse create(CreateTenantRequest request, String actorSubject) {
        String code = request.tenantCode().trim().toLowerCase();
        if (tenantRepository.existsByTenantCodeIgnoreCase(code)) {
            throw new ConflictException("Tenant code already exists: " + code);
        }
        Tenant tenant = Tenant.create(code, request.companyName().trim(),
                StringUtils.hasText(request.industry()) ? request.industry().trim() : null, actorSubject);
        Tenant saved = tenantRepository.save(tenant);
        auditClient.record(actorSubject, "TENANT_REGISTERED", saved.getTenantId(),
                Map.of("tenantCode", saved.getTenantCode(), "companyName", saved.getCompanyName()));
        return TenantResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public TenantResponse get(UUID tenantId) {
        return TenantResponse.from(requireTenant(tenantId));
    }

    @Transactional(readOnly = true)
    public Page<TenantResponse> list(TenantStatus status, Pageable pageable) {
        Page<Tenant> tenants = status == null ? tenantRepository.findAll(pageable)
                : tenantRepository.findAllByStatus(status, pageable);
        return tenants.map(TenantResponse::from);
    }

    @Transactional
    public TenantResponse changeStatus(UUID tenantId, TenantStatus nextStatus, String actorSubject) {
        Tenant tenant = requireTenant(tenantId);
        TenantStatus previous = tenant.getStatus();
        tenant.changeStatus(nextStatus);
        Tenant saved = tenantRepository.save(tenant);
        auditClient.record(actorSubject, "TENANT_STATUS_CHANGED", tenantId,
                Map.of("previousStatus", previous.name(), "newStatus", saved.getStatus().name()));
        return TenantResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Tenant requireTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + tenantId));
    }
}
