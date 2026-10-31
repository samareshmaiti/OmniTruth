import { Routes } from '@angular/router';
import { HomeComponent } from './component/home/home.component';
import { LoginComponent } from './component/login/login.component';
import { ContactComponent } from './component/omniDtls/contact/contact.component';
import { AboutComponent } from './component/omniDtls/about/about.component';
import { SupAdminComponent } from './component/sup-admin/sup-admin.component';
import { NotfoundComponent } from './component/omniDtls/notfound/notfound.component';

export const routes: Routes = [
    { path: '', redirectTo: '/home', pathMatch: 'full' }, // Default route
    { path: 'home', component: HomeComponent },
    { path: 'login', component: LoginComponent },
    { path: 'contact', component: ContactComponent },
    { path: 'superAdmin', component: SupAdminComponent },
    { path: 'about', component: AboutComponent }, // Lazy-loaded route
    { path: 'error', component: NotfoundComponent } // Wildcard route for a 404 page
];
