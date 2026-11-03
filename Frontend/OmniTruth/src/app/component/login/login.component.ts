
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
  exp?: number;
}

interface TenantResolutionResponse {
  tenantId: string;
  tenantSlug: string;
  companyName: string;
  entraTenantId: string;
  status: string;
}

interface UserContextResponse {
  tenantId: string;
  tenantSlug: string;
  companyName: string;
  roles: string[];
  landingPath?: string;
}

interface LoginState {
  flow: 'platform' | 'customer';
  tenantSlug?: string;
}

interface LoginOptions {
  loginHint?: string;
  prompt?: 'select_account' | 'login' | 'none';
}


// Signals that MSAL is starting an interactive token redirect.
// This is not an authentication failure.
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

        /*
         * Normal visit to /login:
         * No redirect result exists.
         * Do not automatically reuse a cached account
         * and classify it as a Super Admin.
         */
        if (!result) {
          this.isLoading = false;
          return;
        }

        const account: AccountInfo | null = result.account ?? null;

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

          } else if (
            loginState.flow === 'customer' &&
            loginState.tenantSlug
          ) {

            await this.handleCustomerLogin(
              account,
              loginState.tenantSlug,
              loginState
            );

          } else {

            throw new Error(
              'Unable to determine the Microsoft login flow.'
            );
          }

        } catch (error) {

          if (error instanceof RedirectInProgressError) {
            return;
          }

          console.error('Login processing failed:', error);

          this.isLoading = false;
          this.statusMessage = '';

          this.errorMessage =
            error instanceof Error
              ? error.message
              : 'Unable to complete login. Please contact your administrator.';
        }
      },

      error: (error) => {

        console.error('Microsoft redirect error:', error);

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

        const state = JSON.parse(result.state) as LoginState;

        if (state.flow === 'platform') {
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
        console.warn('Unable to parse Microsoft login state.');
      }
    }

    /*
     * Optional fallback for customer-specific login routes.
     * Do not default an unknown flow to platform login.
     */
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
  // PLATFORM SUPER ADMIN LOGIN
  // --------------------------------------------------

  onContinue(loginForm: NgForm): void {

    if (loginForm.invalid) {
      loginForm.control.markAllAsTouched();
      return;
    }

    this.startLogin(
      { flow: 'platform' },

      // Vendor tenant authority.
      environment.azure.authority,

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

    this.statusMessage = 'Verifying administrator account...';

    // Verify vendor tenant.
    if (
      account.tenantId?.toLowerCase() !==
      environment.azure.tenantId.toLowerCase()
    ) {
      throw new Error(
        'Invalid OmniTruth administrator tenant.'
      );
    }

    // Verify Super Admin application role.
    const claims = account.idTokenClaims as {
      roles?: string[];
    } | undefined;

    const roles = claims?.roles ?? [];

    if (!roles.includes('PLATFORM_SUPER_ADMIN')) {
      throw new Error(
        'Platform Super Admin role is not assigned.'
      );
    }

    // Obtain Platform API access token.
    const apiToken = await this.acquireApiToken(
      account,
      environment.azure.authority,
      loginState
    );

    const apiClaims =
      this.decodeAccessTokenClaims(apiToken);

    this.validatePlatformApiClaims(
      apiClaims,
      environment.azure.tenantId
    );

    this.isLoading = false;

    this.statusMessage =
      'Welcome to OmniTruth Super Admin Portal';

    await this.router.navigate(['/superAdmin']);
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

    void this.resolveTenantAndStartLogin(normalizedSlug);
  }


  private async resolveTenantAndStartLogin(
    tenantSlug: string
  ): Promise<void> {

    this.isLoading = true;
    this.errorMessage = '';
    this.statusMessage = 'Checking your organisation...';

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

      // A customer must not resolve to the vendor directory.
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

        // Existing customer: use its already-mapped tenant.
        customerAuthority,

        {
          // Do not force the invitation email or personal Gmail.
          prompt: 'select_account'
        }
      );

    } catch (error) {

      console.error('Tenant resolution failed:', error);

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
    tenantSlug: string,
    loginState: LoginState
  ): Promise<void> {

    this.statusMessage =
      'Verifying your organisation...';

    // Resolve trusted tenant information again.
    const tenant = await firstValueFrom(
      this.http.get<TenantResolutionResponse>(
        `${environment.tenantManagement.baseUrl}` +
        `/api/public/v1/tenants/resolve/` +
        `${encodeURIComponent(tenantSlug)}`
      )
    );

    if (tenant.status !== 'ACTIVE') {
      throw new Error(
        'Customer organisation is not active.'
      );
    }

    // Verify Entra tenant ID from authenticated account.
    if (
      account.tenantId?.toLowerCase() !==
      tenant.entraTenantId.toLowerCase()
    ) {
      throw new Error(
        'You signed in with an account from the wrong organisation.'
      );
    }

    const customerAuthority =
      `https://login.microsoftonline.com/${tenant.entraTenantId}`;

    // Obtain access token for the customer tenant.
    const apiToken = await this.acquireApiToken(
      account,
      customerAuthority,
      loginState
    );

    const tokenClaims =
      this.decodeAccessTokenClaims(apiToken);

    this.validatePlatformApiClaims(
      tokenClaims,
      tenant.entraTenantId
    );

    this.statusMessage =
      'Loading your organisation profile...';

    // Existing platform context endpoint.
    const context = await firstValueFrom(
      this.http.get<UserContextResponse>(
        `${environment.platformApi.baseUrl}` +
        `/api/platform/v1/me/context`,
        {
          headers: new HttpHeaders({
            Authorization: `Bearer ${apiToken}`
          })
        }
      )
    );

    // Verify backend tenant resolution.
    if (
      context.tenantSlug?.toLowerCase() !==
      tenant.tenantSlug.toLowerCase()
    ) {
      throw new Error(
        'Organisation context verification failed.'
      );
    }

    if (!context.roles || context.roles.length === 0) {
      throw new Error(
        'No application roles are assigned.'
      );
    }

    this.isLoading = false;

    // Company Admin.
    if (context.roles.includes('COMPANY_ADMIN')) {

      this.statusMessage =
        `Welcome to ${context.companyName}`;

      await this.router.navigate([
        '/app',
        context.tenantSlug,
        'dashboard'
      ]);

      return;
    }

    // Employee.
    if (context.roles.includes('COMPANY_USER')) {

      this.statusMessage =
        `Welcome to ${context.companyName}`;

      await this.router.navigate([
        '/app',
        context.tenantSlug,
        'home'
      ]);

      return;
    }

    throw new Error(
      'Your account does not have an authorised OmniTruth role.'
    );
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

      // Authority is explicitly selected for each flow.
      authority,

      redirectUri:
        environment.azure.redirectUri,

      scopes: [
        'openid',
        'profile',
        environment.platformApi.scope
      ],

      // Preserve the original flow after redirect.
      state: JSON.stringify(state),

      prompt: options.prompt ?? 'select_account',

      ...(options.loginHint
        ? { loginHint: options.loginHint }
        : {})
    };

    this.msalService.loginRedirect(request).subscribe({

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
  // PLATFORM API ACCESS TOKEN
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

        // Important: preserve platform/customer flow.
        state: JSON.stringify(loginState),

        prompt: 'select_account'
      };

      try {

        await firstValueFrom(
          this.msalService.acquireTokenRedirect(request)
        );

      } catch (redirectError) {

        console.error(
          'API token redirect failed:',
          redirectError
        );

        throw redirectError;
      }

      // The browser should now be redirecting to Microsoft.
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
      claims.aud !== environment.platformApi.clientId
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

    // Diagnostics only. Backend must independently validate
    // token signature, issuer, audience, expiry and tenant.
    console.log('Platform API token diagnostics:', {
      aud: claims.aud,
      tid: claims.tid,
      roles: claims.roles,
      scp: claims.scp,
      ver: claims.ver
    });
  }


  // --------------------------------------------------
  // UI HELPERS
  // --------------------------------------------------

  clearStatus(): void {
    this.statusMessage = '';
    this.errorMessage = '';
  }

}
