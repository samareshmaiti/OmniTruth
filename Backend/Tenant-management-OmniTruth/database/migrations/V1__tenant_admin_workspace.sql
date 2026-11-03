-- New Tenant Admin workspace tables. Existing tenants table is not altered.
CREATE TABLE IF NOT EXISTS tenant_identity_mappings (
    mapping_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL UNIQUE REFERENCES tenants(tenant_id),
    entra_tenant_id VARCHAR(36) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS tenant_clients (
    client_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(tenant_id),
    client_name VARCHAR(180) NOT NULL,
    contact_email VARCHAR(254),
    external_reference VARCHAR(120),
    notes VARCHAR(2000),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_tenant_clients_tenant ON tenant_clients(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_clients_name ON tenant_clients(client_name);

CREATE TABLE IF NOT EXISTS tenant_api_endpoints (
    endpoint_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(tenant_id),
    endpoint_name VARCHAR(160) NOT NULL,
    base_url VARCHAR(1000) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    auth_type VARCHAR(30) NOT NULL,
    secret_reference VARCHAR(300),
    description VARCHAR(1000),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_tenant_api_endpoints_tenant ON tenant_api_endpoints(tenant_id);

CREATE TABLE IF NOT EXISTS tenant_tool_definitions (
    tool_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(tenant_id),
    endpoint_id UUID NOT NULL REFERENCES tenant_api_endpoints(endpoint_id),
    tool_name VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    input_schema TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_tenant_tools_tenant ON tenant_tool_definitions(tenant_id);
