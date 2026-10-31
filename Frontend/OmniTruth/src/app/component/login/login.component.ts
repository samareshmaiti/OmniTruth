
import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { MsalService } from '@azure/msal-angular';
import { InteractionRequiredAuthError } from '@azure/msal-browser';

import { environment } from '../../../../environment';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {

  email = '';
  statusMessage = '';
  errorMessage = '';
  isLoading = false;

  readonly currentYear = new Date().getFullYear();

  constructor(
    private router: Router,
    private msalService: MsalService
  ) {}


  // ==========================================
  // STEP 1: START MICROSOFT LOGIN
  // ==========================================

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

      loginHint: this.email.trim(),

      // Authenticate the user first.
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


  // ==========================================
  // STEP 2: ACQUIRE PLATFORM API ACCESS TOKEN
  // ==========================================

  acquirePlatformApiToken(): void {

    const activeAccount =
      this.msalService.instance.getActiveAccount();

    const accounts =
      this.msalService.instance.getAllAccounts();

    const account =
      activeAccount ?? (accounts.length === 1 ? accounts[0] : null);

    if (!account) {

      this.errorMessage =
        'No authenticated Microsoft account found. Please sign in.';

      this.router.navigate(['/login']);

      return;
    }

    // Set active account if it has not already been selected.
    if (!activeAccount) {
      this.msalService.instance.setActiveAccount(account);
    }

    this.msalService.acquireTokenSilent({

      account,

      // Request token specifically for OmniTruth Platform API.
      scopes: [environment.platformApi.scope]

    }).subscribe({

      next: (result) => {

        this.isLoading = false;

        this.statusMessage =
          'Microsoft authentication successful.';

        // Do not print or store the access token manually.
        console.log('Platform API access token obtained.');

        // Next: call the backend /me endpoint
        // and verify PLATFORM_SUPER_ADMIN authorization.

      },

      error: (error) => {

        console.error('Platform API token acquisition failed:', error);

        if (error instanceof InteractionRequiredAuthError) {

          // Microsoft requires additional interaction.
          this.msalService.acquireTokenRedirect({
            account,
            scopes: [environment.platformApi.scope]
          }).subscribe({
            error: redirectError => {
              console.error('Token redirect failed:', redirectError);
              this.errorMessage = 'Unable to obtain API access.';
            }
          });

        } else {

          this.errorMessage =
            'Unable to obtain OmniTruth Platform API access token.';

        }

      }

    });

  }


  // ==========================================
  // STEP 3: CLEAR LOGIN STATUS
  // ==========================================

  clearStatus(): void {
    this.statusMessage = '';
    this.errorMessage = '';
  }

}
