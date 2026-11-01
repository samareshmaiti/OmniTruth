CREATE TABLE tenants (
    tenant_id UUID PRIMARY KEY,
    tenant_code VARCHAR(80) NOT NULL UNIQUE,
    company_name VARCHAR(180) NOT NULL,
    industry VARCHAR(120),
    status VARCHAR(32) NOT NULL,
    created_by VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_tenants_status ON tenants(status);
CREATE INDEX idx_tenants_company_name ON tenants(company_name);
