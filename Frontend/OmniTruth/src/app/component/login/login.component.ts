
import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';

import {
  ActivatedRoute,
  Router,
  RouterLink
} from '@angular/router';

import {
  HttpClient,
  HttpErrorResponse,
  HttpHeaders
} from '@angular/common/http';

import { MsalService } from '@azure/msal-angular';

import {
  AccountInfo,
  AuthenticationResult,
  RedirectRequest
} from '@azure/msal-browser';

import { firstValueFrom } from 'rxjs';

import { environment } from '../../../../environment';

// --------------------------------------------------
// INTERFACES
// --------------------------------------------------

interface PlatformApiTokenClaims {
  aud?: string;
  iss?: string;
  ver?: string;
  roles?: string[];
  scp?: string;
  tid?: string;
  oid?: string;
  sub?: string;
  exp?: number;
}

interface TenantResolutionResponse {
  tenantId: string;
  tenantSlug: string;
  companyName: string;
  entraTenantId: string;
  status: string;
}

interface TenantDashboardResponse {
  tenant: {
    tenantId: string;
    tenantCode: string;
    companyName: string;
    status: string;
  };
  totalClients: number;
  activeClients: number;
  totalApiEndpoints: number;
  enabledApiEndpoints: number;
  totalTools: number;
  enabledTools: number;
}

interface LoginState {
  flow: 'auto' | 'platform' | 'customer';
  tenantSlug?: string;
  email?: string;
}

interface LoginOptions {
  loginHint?: string;
  prompt?: 'select_account' | 'login' | 'none';
}

class RedirectInProgressError extends Error {
  constructor() {
    super('Redirecting to Microsoft to complete authentication.');
    this.name = 'RedirectInProgressError';
  }
}

// --------------------------------------------------
// COMPONENT
// --------------------------------------------------

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {

  email = '';

  statusMessage = '';
  errorMessage = '';

  isLoading = false;

  readonly currentYear = new Date().getFullYear();

  // Microsoft consumer account tenant.
  private readonly consumerTenantId =
    '9188040d-6c67-4c5b-b112-36a304b66dad';

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private http: HttpClient,
    private msalService: MsalService
  ) {}

  // --------------------------------------------------
  // INITIALIZATION / MICROSOFT REDIRECT
  // --------------------------------------------------

  ngOnInit(): void {
    this.handleMicrosoftRedirect();
  }

  private handleMicrosoftRedirect(): void {

    this.isLoading = true;

    this.msalService.handleRedirectObservable({
      navigateToLoginRequestUrl: false
    }).subscribe({

      next: async (result: AuthenticationResult | null) => {

        if (!result) {
          this.isLoading = false;
          return;
        }

        const account: AccountInfo | null =
          result.account ?? null;

        if (!account) {
          this.isLoading = false;
          this.errorMessage =
            'Microsoft account information is missing.';
          return;
        }

        this.msalService.instance.setActiveAccount(account);

        try {

          const loginState = this.getLoginState(result);

          if (loginState.flow === 'platform') {

            await this.handlePlatformLogin(
              account,
              loginState
            );

          } else if (loginState.flow === 'customer') {

            if (!loginState.tenantSlug) {
              throw new Error(
                'Customer organisation information is missing.'
              );
            }

            await this.handleCustomerLogin(
              account,
              loginState,
              loginState.tenantSlug
            );

          } else if (loginState.flow === 'auto') {

            const authenticatedTenantId =
              account.tenantId?.toLowerCase();

            if (!authenticatedTenantId) {
              throw new Error(
                'Microsoft did not return an authenticated tenant ID.'
              );
            }

            // Personal Microsoft accounts are routed to
            // the Platform Super Admin verification flow.
            if (
              authenticatedTenantId ===
              this.consumerTenantId
            ) {

              await this.handlePlatformLogin(
                account,
                {
                  ...loginState,
                  flow: 'platform'
                }
              );

            } else if (
              authenticatedTenantId ===
              environment.azure.tenantId.toLowerCase()
            ) {

              // OmniTruth vendor organizational account.
              await this.handlePlatformLogin(
                account,
                {
                  ...loginState,
                  flow: 'platform'
                }
              );

            } else {

              // Other organizational accounts use customer login.
              await this.handleCustomerLogin(
                account,
                loginState
              );
            }

          } else {

            throw new Error(
              'Unable to determine the Microsoft login flow.'
            );
          }

        } catch (error) {

          if (error instanceof RedirectInProgressError) {
            return;
          }

          console.error(
            'Login processing failed:',
            error
          );

          this.isLoading = false;
          this.statusMessage = '';

          this.errorMessage =
            error instanceof Error
              ? error.message
              : 'Unable to complete login. Please contact your administrator.';
        }
      },

      error: (error) => {

        console.error(
          'Microsoft redirect error:',
          error
        );

        this.isLoading = false;
        this.statusMessage = '';

        this.errorMessage =
          'Microsoft sign-in could not be completed.';
      }
    });
  }

  // --------------------------------------------------
  // RECOVER LOGIN FLOW FROM OAUTH STATE
  // --------------------------------------------------

  private getLoginState(
    result: AuthenticationResult
  ): LoginState {

    if (result.state) {

      try {

        const state = JSON.parse(
          result.state
        ) as LoginState;

        if (state.flow === 'platform') {
          return state;
        }

        if (
          state.flow === 'auto' &&
          typeof state.email === 'string' &&
          state.email.trim()
        ) {
          return state;
        }

        if (
          state.flow === 'customer' &&
          typeof state.tenantSlug === 'string' &&
          state.tenantSlug.trim()
        ) {
          return state;
        }

      } catch {
        console.warn(
          'Unable to parse Microsoft login state.'
        );
      }
    }

    const tenantSlug =
      this.route.snapshot.paramMap.get('tenantSlug');

    if (tenantSlug) {
      return {
        flow: 'customer',
        tenantSlug
      };
    }

    throw new Error(
      'The login state is missing or invalid. Please restart sign-in.'
    );
  }

  // --------------------------------------------------
  // COMMON LOGIN ENTRY
  // --------------------------------------------------

  onCommonContinue(loginForm: NgForm): void {

    if (loginForm.invalid) {
      loginForm.control.markAllAsTouched();
      return;
    }

    const normalizedEmail =
      this.email.trim().toLowerCase();

    if (!normalizedEmail) {
      this.errorMessage =
        'Enter your work email address.';
      return;
    }

    this.startLogin(
      {
        flow: 'auto',
        email: normalizedEmail
      },
      environment.azure.commonAuthority,
      {
        loginHint: normalizedEmail,
        prompt: 'select_account'
      }
    );
  }

  // --------------------------------------------------
  // PLATFORM SUPER ADMIN LOGIN
  // --------------------------------------------------

  onContinue(loginForm: NgForm): void {

    if (loginForm.invalid) {
      loginForm.control.markAllAsTouched();
      return;
    }

    this.startLogin(
      {
        flow: 'platform'
      },
      environment.azure.commonAuthority,
      {
        loginHint: this.email.trim(),
        prompt: 'select_account'
      }
    );
  }

  private async handlePlatformLogin(
    account: AccountInfo,
    loginState: LoginState
  ): Promise<void> {

    this.statusMessage =
      'Verifying administrator account...';

    const authenticatedTenantId =
      account.tenantId?.toLowerCase();

    if (!authenticatedTenantId) {
      throw new Error(
        'Microsoft did not return an authenticated tenant ID.'
      );
    }

    const isConsumerAccount =
      authenticatedTenantId === this.consumerTenantId;

    const isVendorAccount =
      authenticatedTenantId ===
      environment.azure.tenantId.toLowerCase();

    if (!isConsumerAccount && !isVendorAccount) {
      throw new Error(
        'This account is not authorized for OmniTruth Platform Administration.'
      );
    }

    const apiAuthority = isConsumerAccount
      ? environment.azure.commonAuthority
      : environment.azure.authority;

    const apiToken = await this.acquireApiToken(
      account,
      apiAuthority,
      {
        ...loginState,
        flow: 'platform'
      }
    );

    const apiClaims =
      this.decodeAccessTokenClaims(apiToken);

    this.validatePlatformApiClaims(
      apiClaims,
      isConsumerAccount
        ? this.consumerTenantId
        : environment.azure.tenantId
    );

    this.statusMessage =
      'Checking Platform Administrator permissions...';

    // Existing endpoint. No new endpoint is introduced.
    try {

      await firstValueFrom(
        this.http.get(
          `${environment.platformApi.baseUrl}` +
          `/api/platform/v1/tenants?page=0&size=1`,
          {
            headers: new HttpHeaders({
              Authorization: `Bearer ${apiToken}`
            })
          }
        )
      );

    } catch (error) {

      if (
        error instanceof HttpErrorResponse &&
        error.status === 401
      ) {
        throw new Error(
          'Platform API rejected the Microsoft access token. Check the API token issuer, signature, audience and scope configuration.'
        );
      }

      if (
        error instanceof HttpErrorResponse &&
        error.status === 403
      ) {
        throw new Error(
          'Your Microsoft account is authenticated, but it is not registered as an active OmniTruth Platform Super Admin.'
        );
      }

      throw error;
    }

    this.isLoading = false;

    this.statusMessage =
      'Welcome to OmniTruth Super Admin Portal';

    await this.router.navigate([
      '/superAdmin'
    ]);
  }

  // --------------------------------------------------
  // EXISTING CUSTOMER LOGIN
  // --------------------------------------------------

  onCustomerContinue(
    tenantSlug: string,
    loginForm?: NgForm
  ): void {

    if (loginForm && loginForm.invalid) {
      loginForm.control.markAllAsTouched();
      return;
    }

    const normalizedSlug =
      tenantSlug.trim().toLowerCase();

    if (!normalizedSlug) {
      this.errorMessage =
        'Customer organisation is required.';
      return;
    }

    void this.resolveTenantAndStartLogin(
      normalizedSlug
    );
  }

  private async resolveTenantAndStartLogin(
    tenantSlug: string
  ): Promise<void> {

    this.isLoading = true;
    this.errorMessage = '';
    this.statusMessage =
      'Checking your organisation...';

    try {

      const tenant = await firstValueFrom(
        this.http.get<TenantResolutionResponse>(
          `${environment.tenantManagement.baseUrl}` +
          `/api/public/v1/tenants/resolve/` +
          `${encodeURIComponent(tenantSlug)}`
        )
      );

      if (tenant.status !== 'ACTIVE') {
        throw new Error(
          'This organisation is not active.'
        );
      }

      if (!tenant.entraTenantId) {
        throw new Error(
          'SSO is not configured for this organisation.'
        );
      }

      if (
        tenant.entraTenantId.toLowerCase() ===
        environment.azure.tenantId.toLowerCase()
      ) {
        throw new Error(
          'Customer Entra mapping incorrectly points to the OmniTruth vendor directory.'
        );
      }

      const customerAuthority =
        `https://login.microsoftonline.com/${tenant.entraTenantId}`;

      this.startLogin(
        {
          flow: 'customer',
          tenantSlug: tenant.tenantSlug
        },
        customerAuthority,
        {
          prompt: 'select_account'
        }
      );

    } catch (error) {

      console.error(
        'Tenant resolution failed:',
        error
      );

      this.isLoading = false;
      this.statusMessage = '';

      this.errorMessage =
        error instanceof Error
          ? error.message
          : 'Unable to find an active organisation with configured SSO.';
    }
  }

  private async handleCustomerLogin(
    account: AccountInfo,
    loginState: LoginState,
    tenantSlug?: string
  ): Promise<void> {

    this.statusMessage =
      'Verifying your organisation...';

    const entraTenantId =
      account.tenantId?.trim();

    if (!entraTenantId) {
      throw new Error(
        'The Microsoft account does not contain an organisation tenant ID.'
      );
    }

    if (
      entraTenantId.toLowerCase() ===
      environment.azure.tenantId.toLowerCase()
    ) {
      throw new Error(
        'The OmniTruth vendor account could not be verified as a Super Admin.'
      );
    }

    // Personal Microsoft accounts are not customer
    // organizational identities.
    if (
      entraTenantId.toLowerCase() ===
      this.consumerTenantId
    ) {
      throw new Error(
        'Please use your organisation Microsoft 365 account to access the customer workspace.'
      );
    }

    let resolvedTenant: TenantResolutionResponse | null = null;

    if (tenantSlug) {

      resolvedTenant = await firstValueFrom(
        this.http.get<TenantResolutionResponse>(
          `${environment.tenantManagement.baseUrl}` +
          `/api/public/v1/tenants/resolve/` +
          `${encodeURIComponent(tenantSlug)}`
        )
      );

      if (resolvedTenant.status !== 'ACTIVE') {
        throw new Error(
          'Customer organisation is not active.'
        );
      }

      if (
        resolvedTenant.entraTenantId.toLowerCase() !==
        entraTenantId.toLowerCase()
      ) {
        throw new Error(
          'You signed in with an account from the wrong organisation.'
        );
      }
    }

    const customerAuthority =
      `https://login.microsoftonline.com/${entraTenantId}`;

    const apiToken = await this.acquireApiToken(
      account,
      customerAuthority,
      loginState
    );

    const tokenClaims =
      this.decodeAccessTokenClaims(apiToken);

    this.validatePlatformApiClaims(
      tokenClaims,
      entraTenantId
    );

    this.statusMessage =
      'Verifying your OmniTruth workspace access...';

    let dashboard: TenantDashboardResponse;

    try {

      dashboard = await firstValueFrom(
        this.http.get<TenantDashboardResponse>(
          `${environment.tenantManagement.baseUrl}` +
          `/api/tenant/v1/dashboard`,
          {
            headers: new HttpHeaders({
              Authorization: `Bearer ${apiToken}`
            })
          }
        )
      );

    } catch (error) {

      if (
        error instanceof HttpErrorResponse &&
        [401, 403].includes(error.status)
      ) {
        throw new Error(
          'Microsoft sign-in succeeded, but this identity is not linked to an active OmniTruth Company Admin account. Complete your invitation or contact your OmniTruth administrator.'
        );
      }

      throw error;
    }

    const tenant = dashboard?.tenant;

    if (!tenant?.tenantCode || !tenant.companyName) {
      throw new Error(
        'Tenant Management returned an incomplete workspace response.'
      );
    }

    if (tenant.status?.toUpperCase() !== 'ACTIVE') {
      throw new Error(
        'Your OmniTruth organisation is not active. Contact your administrator.'
      );
    }

    if (
      resolvedTenant &&
      tenant.tenantCode.toLowerCase() !==
      resolvedTenant.tenantSlug.toLowerCase()
    ) {
      throw new Error(
        'Organisation context verification failed.'
      );
    }

    this.isLoading = false;

    this.statusMessage =
      `Welcome to ${tenant.companyName}`;

    await this.router.navigate([
      '/app',
      tenant.tenantCode,
      'dashboard'
    ]);
  }

  // --------------------------------------------------
  // COMMON MICROSOFT LOGIN
  // --------------------------------------------------

  private startLogin(
    state: LoginState,
    authority: string,
    options: LoginOptions = {}
  ): void {

    this.isLoading = true;

    this.statusMessage =
      'Redirecting to Microsoft...';

    this.errorMessage = '';

    const request: RedirectRequest = {

      authority,

      redirectUri:
        environment.azure.redirectUri,

      scopes: [
        'openid',
        'profile',
        environment.platformApi.scope
      ],

      state: JSON.stringify(state),

      prompt:
        options.prompt ?? 'select_account',

      ...(options.loginHint
        ? { loginHint: options.loginHint }
        : {})
    };

    this.msalService.loginRedirect(
      request
    ).subscribe({

      error: (error) => {

        console.error(
          'Microsoft login failed:',
          error
        );

        this.isLoading = false;
        this.statusMessage = '';

        this.errorMessage =
          'Unable to start Microsoft sign-in. Please try again.';
      }
    });
  }

  // --------------------------------------------------
  // API ACCESS TOKEN
  // --------------------------------------------------

  private async acquireApiToken(
    account: AccountInfo,
    authority: string,
    loginState: LoginState
  ): Promise<string> {

    try {

      const result = await firstValueFrom(
        this.msalService.acquireTokenSilent({

          account,
          authority,

          scopes: [
            environment.platformApi.scope
          ]
        })
      );

      if (!result.accessToken) {
        throw new Error(
          'Microsoft returned an empty API access token.'
        );
      }

      return result.accessToken;

    } catch (silentError) {

      console.warn(
        'Silent API token acquisition failed. Starting interactive token acquisition.',
        silentError
      );

      const request: RedirectRequest = {

        account,
        authority,

        redirectUri:
          environment.azure.redirectUri,

        scopes: [
          environment.platformApi.scope
        ],

        state: JSON.stringify(loginState),

        prompt: 'select_account'
      };

      try {

        await firstValueFrom(
          this.msalService.acquireTokenRedirect(
            request
          )
        );

      } catch (redirectError) {

        console.error(
          'API token redirect failed:',
          redirectError
        );

        throw redirectError;
      }

      throw new RedirectInProgressError();
    }
  }

  // --------------------------------------------------
  // JWT CLAIM DECODING
  // --------------------------------------------------

  private decodeAccessTokenClaims(
    accessToken: string
  ): PlatformApiTokenClaims {

    const parts = accessToken.split('.');

    if (parts.length !== 3) {
      throw new Error(
        'Invalid access token format.'
      );
    }

    const base64 = parts[1]
      .replace(/-/g, '+')
      .replace(/_/g, '/');

    const padded = base64.padEnd(
      Math.ceil(base64.length / 4) * 4,
      '='
    );

    const decoded = atob(padded);

    const bytes = Uint8Array.from(
      decoded,
      char => char.charCodeAt(0)
    );

    return JSON.parse(
      new TextDecoder().decode(bytes)
    ) as PlatformApiTokenClaims;
  }

  // --------------------------------------------------
  // FRONTEND TOKEN CHECKS
  // --------------------------------------------------

  private validatePlatformApiClaims(
    claims: PlatformApiTokenClaims,
    expectedTenantId: string
  ): void {

    if (
      claims.aud !==
      environment.platformApi.clientId
    ) {
      throw new Error(
        'Unexpected Platform API audience.'
      );
    }

    if (
      claims.tid?.toLowerCase() !==
      expectedTenantId.toLowerCase()
    ) {
      throw new Error(
        'Unexpected Entra tenant in access token.'
      );
    }

    if (
      !claims.scp?.split(' ').includes('access_as_user')
    ) {
      throw new Error(
        'Platform API delegated scope is missing.'
      );
    }

    if (
      claims.exp &&
      claims.exp * 1000 <= Date.now()
    ) {
      throw new Error(
        'Platform API access token has expired.'
      );
    }

    // Diagnostics only. These decoded claims are not
    // used as proof of authorization.
    console.log(
      'API token diagnostics:',
      {
        aud: claims.aud,
        tid: claims.tid,
        roles: claims.roles,
        scp: claims.scp,
        ver: claims.ver,
        iss: claims.iss
      }
    );
  }

  // --------------------------------------------------
  // UI HELPERS
  // --------------------------------------------------

  clearStatus(): void {
    this.statusMessage = '';
    this.errorMessage = '';
  }
}
