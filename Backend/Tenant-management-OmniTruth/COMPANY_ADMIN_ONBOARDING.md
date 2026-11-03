# Company Admin invitation onboarding

This change adds the backend steps for the following flow:

1. Validate an invitation link.
2. Accept the invitation.
3. Authenticate with Microsoft Entra ID.
4. Bind the validated Entra tenant/object identity to the invited OmniTruth tenant.
5. Grant the OmniTruth-managed `COMPANY_ADMIN` role and continue to the existing tenant dashboard.

Existing Kafka invitation processing, email sending, tenant dashboard APIs, profile APIs, and Entra app-role claim handling are retained. The fuller `controller.TenantWorkspaceController` remains the active route controller; the older duplicate class under `web` is retained as source but is no longer registered as a second Spring controller, avoiding duplicate mappings for the same URLs.

## APIs

### 1. Validate invitation

`GET /api/public/v1/invitations/validate?token={rawInvitationToken}`

No bearer token is required. The server hashes the supplied token and looks up the invitation. It returns only the invited organization's display details and invitation state. Responses are marked `Cache-Control: no-store`.

### 2. Accept invitation

`POST /api/public/v1/invitations/accept`

Request:

```json
{
  "token": "raw-invitation-token-from-email"
}
```

No bearer token is required. The status changes from `PENDING` to `ACCEPTED_PENDING_SSO`. This step does not grant dashboard access.

### 3. Complete onboarding after Microsoft SSO

`POST /api/public/v1/invitations/complete`

Request:

```json
{
  "invitationToken": "raw-invitation-token-from-email"
}
```

Header:

```http
Authorization: Bearer <Microsoft Entra access token for the OmniTruth API>
```

The access token must pass the existing API-audience and `access_as_user` scope checks and must contain valid `tid` and `oid` GUID claims. The backend derives those values from the validated token; it does not accept them from the request body.

The invitation's requested role must be `COMPANY_ADMIN`. The service then:

- Prevents one Entra tenant from being linked to two OmniTruth tenants.
- Prevents one OmniTruth tenant from being linked to a different Entra tenant.
- Creates the `tenant_identity_mappings` record if needed.
- Creates or activates the user using the immutable `(tid, oid)` identity pair.
- Stores the invited email separately from the Entra object identity, so the two email addresses do not have to be identical.
- Assigns the OmniTruth-managed `COMPANY_ADMIN` role and marks the invitation `ACCEPTED`.

The endpoint supports retry by the same Entra identity after completion. A different identity cannot reuse a completed invitation to take over the role.

## Invitation statuses

- `PENDING`: invitation created by the existing Kafka consumer.
- `ACCEPTED_PENDING_SSO`: invitee accepted the invitation and must authenticate.
- `ACCEPTED`: Entra identity is linked and onboarding is complete.

Expired invitations are rejected with HTTP 410. Invalid tokens return HTTP 404. Identity/mapping conflicts return HTTP 409.

## Security behavior

The normal tenant API authentication path remains restricted to Entra tenants that already have an active `tenant_identity_mappings` record. A separate, narrowly scoped issuer resolver permits a valid multitenant Entra access token on only `/api/public/v1/invitations/complete`, so the first tenant mapping can be established. It still validates the Microsoft issuer signature, issuer/`tid` match, configured API audience, token expiry, and `access_as_user` scope.

The JWT authority converter continues to retain existing Entra `roles` claims. It additionally loads an active OmniTruth role by `(tid, oid)` from `tenant_users`. This is what allows the invited first Company Admin to access the existing `/api/tenant/v1/**` APIs without requiring a Microsoft Graph app-role assignment operation.

The backend does not initiate the Microsoft browser login or admin-consent screen. Angular/MSAL must perform the multitenant sign-in/consent flow and then call the completion endpoint with an access token for the configured OmniTruth API. This code does not claim that tenant-wide admin consent has been granted; that depends on the permissions and consent flow configured for the Entra application.

## Database update

An additive migration is included at:

`database/migrations/V2__company_admin_invitation_onboarding.sql`

It adds `accepted_at` to `company_admin_invitations` and creates `tenant_users`. The entity mapping also supports Hibernate schema update in environments where the project's existing JPA schema-update setting is enabled. In environments that manage schema explicitly, run the migration through your normal database deployment process.

## Existing files deliberately retained

- `CompanyAdminInvitationConsumer`
- `CompanyAdminInvitationService` (Kafka event handling, token creation/hash storage, and email delivery)
- Existing tenant workspace controllers and services
- Existing public tenant resolution endpoint
- Existing Entra role-claim authorization behavior

`src/main/resources/application.properties` and `pom.xml` were not changed.
