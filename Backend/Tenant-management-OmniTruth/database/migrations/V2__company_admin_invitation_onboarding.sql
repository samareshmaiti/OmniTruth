-- Additive Company Admin onboarding support. Existing tables and rows are preserved.
DO $$
BEGIN
    IF to_regclass('public.company_admin_invitations') IS NOT NULL THEN
        ALTER TABLE company_admin_invitations
            ADD COLUMN IF NOT EXISTS accepted_at TIMESTAMPTZ;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS tenant_users (
    user_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(tenant_id),
    entra_tenant_id VARCHAR(36) NOT NULL,
    entra_object_id VARCHAR(36) NOT NULL,
    invitation_email VARCHAR(254) NOT NULL,
    app_role VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_tenant_user_entra_identity UNIQUE (entra_tenant_id, entra_object_id)
);

CREATE INDEX IF NOT EXISTS idx_tenant_users_tenant_id ON tenant_users(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_users_status ON tenant_users(status);
