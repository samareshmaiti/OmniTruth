import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink } from '@angular/router';

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
  readonly currentYear = new Date().getFullYear();

  onContinue(form: NgForm): void {
    if (form.invalid) {
      form.control.markAllAsTouched();
      return;
    }

    // Integration point:
    // POST the email to your Identity & Authentication service, for example:
    // POST /api/v1/auth/sso/discover { "email": this.email }
    // The backend should resolve a verified tenant domain and return a trusted
    // OIDC authorization URL. Do not choose an IdP or grant tenant access in the browser.
    this.statusMessage =
      'Your email is ready for SSO discovery. Connect this form to the OmniTruth Identity API to look up your organisation and redirect to its configured sign-in provider.';
  }

  clearStatus(): void {
    this.statusMessage = '';
  }
}
