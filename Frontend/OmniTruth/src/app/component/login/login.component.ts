import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { MsalService } from '@azure/msal-angular';
import {
  AuthenticationResult,
  AccountInfo
} from '@azure/msal-browser';

import { environment } from '../../../../environment';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
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
    private msalService: MsalService
  ) {}

  ngOnInit(): void {
    console.log('LoginComponent initialized');

    this.isLoading = true;

    this.msalService.handleRedirectObservable().subscribe({
      next: (result: AuthenticationResult | null) => {
        console.log('MSAL redirect response:', result);

        const account: AccountInfo | null =
          result?.account ??
          this.msalService.instance.getActiveAccount() ??
          this.msalService.instance.getAllAccounts()[0] ??
          null;

        if (!account) {
          this.isLoading = false;
          console.log('No authenticated account found.');
          return;
        }

        this.msalService.instance.setActiveAccount(account);

        console.log('Authenticated user:', account.username);

        this.redirectUserByRole(account);
      },

      error: (error) => {
        console.error('MSAL redirect error:', error);

        this.isLoading = false;
        this.errorMessage =
          'Microsoft sign-in could not be completed.';
      }
    });
  }

  onContinue(loginForm: NgForm): void {
    if (loginForm.invalid) {
      loginForm.control.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.statusMessage = 'Redirecting to Microsoft...';
    this.errorMessage = '';

    this.msalService.loginRedirect({
      authority: environment.azure.authority,

      // Must match a URI registered in Entra ID.
      redirectUri: `${window.location.origin}/login`,

      loginHint: this.email.trim(),

      scopes: ['openid', 'profile']
    }).subscribe({
      error: (error) => {
        console.error('Microsoft login failed:', error);

        this.isLoading = false;
        this.statusMessage = '';
        this.errorMessage =
          'Unable to start Microsoft sign-in. Please try again.';
      }
    });
  }

  redirectUserByRole(account: AccountInfo): void {
    const claims = account.idTokenClaims as {
      roles?: string[];
    } | undefined;

    const roles = claims?.roles ?? [];

    console.log('User:', account.username);
    console.log('Assigned roles:', roles);

    this.isLoading = false;

    if (roles.includes('PLATFORM_SUPER_ADMIN')) {
      this.statusMessage =
        'Welcome to OmniTruth Super Admin Portal';

      this.router.navigate(['/superAdmin']);
      return;
    }

    if (
      roles.includes('PLATFORM_ADMIN') ||
      roles.includes('PLATFORM_AUDITOR')
    ) {
      this.router.navigate(['/home']);
      return;
    }

    this.errorMessage =
      'Your account does not have an authorized OmniTruth platform role.';

    this.router.navigate(['/error']);
  }

  clearStatus(): void {
    this.statusMessage = '';
    this.errorMessage = '';
  }
}