import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { AuthGuard } from './guards/auth.guard';
import { GuestGuard } from './guards/guest.guard';

export const routes: Routes = [
	{
        path: 'login', 
        component: LoginComponent,
        canActivate: [GuestGuard]
    },

	{
        path: 'posts',
        loadComponent: () => import('./components/post-list/post-list.component').then(m => m.PostListComponent)
    },

	{ path: '', redirectTo: 'posts', pathMatch: 'full' }
];