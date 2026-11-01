
import { Routes } from '@angular/router';

import { HomeComponent } from './component/home/home.component';
import { LoginComponent } from './component/login/login.component';
import { ContactComponent } from './component/omniDtls/contact/contact.component';
import { AboutComponent } from './component/omniDtls/about/about.component';
import { SuperadminComponent } from './component/dashboard/superadmin/superadmin.component';
import { NotfoundComponent } from './component/omniDtls/notfound/notfound.component';

export const routes: Routes = [

  { path: '', redirectTo: 'login', pathMatch: 'full' },

  { path: 'home', component: HomeComponent },

  { path: 'login', component: LoginComponent },

  { path: 'contact', component: ContactComponent },

  { path: 'superAdmin', component: SuperadminComponent },

  { path: 'about', component: AboutComponent },

  { path: 'error', component: NotfoundComponent },

  { path: '**', redirectTo: 'error' }

];
