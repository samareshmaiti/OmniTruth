import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';

import { HomeComponent } from './component/home/home.component';
import { LoginComponent } from './component/login/login.component';
import { ContactComponent } from './component/omniDtls/contact/contact.component';
import { AboutComponent } from './component/omniDtls/about/about.component';
import { SuperadminComponent } from './component/dashboard/superadmin/superadmin.component';
import { NotfoundComponent } from './component/omniDtls/notfound/notfound.component';

import { InvitationAcceptanceComponent } from './component/company-onboarding/invitation-acceptance/invitation-acceptance.component';
import { EntraAuthorizationComponent } from './component/company-onboarding/entra-authorization/entra-authorization.component';
import { TenantComponent } from './component/dashboard/tenant/tenant.component';
import { tenantAuthGuard } from './guards/tenant-auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' },

  { path: 'home', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'contact', component: ContactComponent },
  { path: 'superAdmin', component: SuperadminComponent },
  { path: 'about', component: AboutComponent },
  { path: 'error', component: NotfoundComponent },

  // Public, one-time invitation link sent to the first Company Admin.
  { path: 'accept-invitation', component: InvitationAcceptanceComponent },

  // MSAL returns to this route after the first Company Admin signs in.
  { path: 'onboarding/entra', component: EntraAuthorizationComponent },

  // Existing company dashboard. MsalGuard checks sign-in; tenantAuthGuard
  // checks active User Management membership, role and tenant slug.
  {
    path: 'app/:tenantSlug/dashboard',
    component: TenantComponent,
    canActivate: [MsalGuard, tenantAuthGuard],
    data: { roles: ['COMPANY_ADMIN'] }
  },

  { path: '**', redirectTo: 'error' }
];
