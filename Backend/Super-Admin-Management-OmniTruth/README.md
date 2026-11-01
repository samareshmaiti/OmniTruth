# OmniTruth Super Admin Service (starter)

A Spring Boot 3 / Java 21 starter for platform-level Super Admin functions. It is intentionally a **platform administration service**, not the whole OmniTruth backend.

## Included

- Register a tenant/company in the platform tenant registry.
- Search/list tenants and read tenant details.
- Move a tenant through controlled lifecycle states: `PENDING`, `SSO_CONFIGURING`, `PROVISIONING`, `ACTIVE`, `SUSPENDED`, `CLOSED`.
- Invite the first `COMPANY_ADMIN` through the separate User Management service, only after the tenant reaches `SSO_CONFIGURING`.
- Validate access JWTs from the central Identity Provider and require the `platform_roles` claim to contain `SUPER_ADMIN` or `PLATFORM_SUPER_ADMIN` for every platform endpoint.
- Use OAuth2 Client Credentials for calls to User Management and Audit Management.
- Write tenant metadata to this service's own PostgreSQL database with Flyway migrations.

## Architecture boundary

This service owns tenant registry data and platform-only tenant lifecycle operations. It does **not** own passwords, customer SSO credentials, JWT issuance, end-user memberships, role assignments, connector credentials, customer records or MCP execution. Those remain with the configured IdP and the corresponding User Management, Identity/SSO Configuration, Access Policy, Connector and MCP services.

The tenant ID passed to User Management is an identifier reference; no cross-service database foreign key is used. Keep each service's database writes private to that service.

## Run locally

Requirements: Java 21, Maven 3.9+, PostgreSQL, and an OIDC provider that can issue access tokens for this API.

Create a database, for example:

```sql
CREATE DATABASE omnitruth_platform_admin;
```

Set the following environment variables before running:

```bash
export DB_URL='jdbc:postgresql://localhost:5432/omnitruth_platform_admin'
export DB_USERNAME='omnitruth'
export DB_PASSWORD='your-local-db-password'
export IDP_ISSUER_URI='https://your-idp.example/realms/omnitruth-platform'
export IDP_TOKEN_URI='https://your-idp.example/realms/omnitruth-platform/protocol/openid-connect/token'
export PLATFORM_API_AUDIENCE='omnitruth-platform-api'
export USER_MGMT_CLIENT_ID='omnitruth-platform-admin'
export USER_MGMT_CLIENT_SECRET='load-from-your-secret-manager'
export AUDIT_MGMT_CLIENT_ID='omnitruth-platform-admin'
export AUDIT_MGMT_CLIENT_SECRET='load-from-your-secret-manager'
export USER_MANAGEMENT_BASE_URL='http://localhost:8082'
export AUDIT_MANAGEMENT_BASE_URL='http://localhost:8084'
```

Then:

```bash
mvn spring-boot:run
```

Do not commit real secrets to `application.yml`, source control or this package. Supply them through Vault / cloud secret manager / Kubernetes Secrets.

## JWT authorization contract

The API expects a signed OIDC access token with:

- `iss`: exact configured platform issuer.
- `aud`: contains `omnitruth-platform-api` (or your configured audience).
- `sub`: stable identity-provider subject for the platform administrator.
- `platform_roles`: an array containing `SUPER_ADMIN` or `PLATFORM_SUPER_ADMIN`.

Do not use a tenant's `tenant_id`, tenant role, email address or an `X-Tenant-ID` header to grant platform-admin access. Customer tenant identities and OmniTruth workforce/platform identities should be configured as separate trust realms or distinct IdP clients/issuers.

## API endpoints

All `/api/platform/v1/**` operations require `ROLE_PLATFORM_SUPER_ADMIN`.

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/platform/v1/tenants` | Register a company/tenant (`PENDING`) |
| `GET` | `/api/platform/v1/tenants?page=0&size=20` | List tenants |
| `GET` | `/api/platform/v1/tenants?status=ACTIVE` | Filter tenants by lifecycle status |
| `GET` | `/api/platform/v1/tenants/{tenantId}` | Read tenant details |
| `PATCH` | `/api/platform/v1/tenants/{tenantId}/status` | Advance/change permitted lifecycle state |
| `POST` | `/api/platform/v1/tenants/{tenantId}/company-admin-invitations` | Invite first Company Admin during `SSO_CONFIGURING` |

Example create tenant:

```bash
curl -X POST http://localhost:8085/api/platform/v1/tenants \
  -H 'Authorization: Bearer <platform-super-admin-access-token>' \
  -H 'Content-Type: application/json' \
  -d '{"tenantCode":"acme-logistics","companyName":"Acme Logistics","industry":"Logistics"}'
```

Example status transition:

```bash
curl -X PATCH http://localhost:8085/api/platform/v1/tenants/<tenant-id>/status \
  -H 'Authorization: Bearer <platform-super-admin-access-token>' \
  -H 'Content-Type: application/json' \
  -d '{"status":"SSO_CONFIGURING"}'
```

Example first Company Admin invitation:

```bash
curl -X POST http://localhost:8085/api/platform/v1/tenants/<tenant-id>/company-admin-invitations \
  -H 'Authorization: Bearer <platform-super-admin-access-token>' \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@acme.example"}'
```

## Service-to-service contracts to align with your other services

- User Management: `POST /api/v1/platform-invitations`, accepting `{tenantId, email, requestedRole, invitedBy}` and returning `CompanyAdminInvitationResponse`.
- Audit Management: `POST /api/v1/audit-events`, accepting `{eventId, actorSubject, action, tenantId, occurredAt, details}`.

Adjust the URI and DTOs to match your independently deployed services. Grant the client credentials only `users.invite` and `audit.write` permissions.

## Important production hardening before deployment

1. Move remote audit delivery behind a **transactional outbox** so an unavailable Audit Management service cannot cause a committed tenant action to lose its audit event. The direct REST call here is a starter integration point, not a durable audit guarantee.
2. Make the User Management invitation endpoint idempotent and use a bounded timeout/retry policy. Consider a Saga/outbox for tenant onboarding across services.
3. Add tenant-domain ownership verification and OIDC configuration workflows before considering a tenant ready for Company Admin SSO. Never infer membership from email domain alone.
4. Apply rate limits, request correlation IDs, structured security logging, CORS allow-lists, TLS/mTLS or workload identity, secret rotation, and platform-admin MFA (prefer WebAuthn/passkeys).
5. Use a platform BFF with secure HttpOnly cookies in the Angular portal; do not put access or refresh tokens in browser local/session storage.
6. Add integration tests against the actual IdP, User Management, Audit Management and database. The included tests cover only the tenant state machine.

This package is a compiling-oriented starter scaffold; the downstream service paths and token claim names must be aligned with your actual deployment contracts before connecting the Angular Super Admin portal.
