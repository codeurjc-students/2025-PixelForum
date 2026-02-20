import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { AuthGuard } from './guards/auth.guard';
import { GuestGuard } from './guards/guest.guard';
import { ProfileComponent } from './components/profile/profile.component';
import { PostListComponent } from './components/post-list/post-list.component';
import { PostDetailComponent } from './components/post-detail/post-detail.component';

export const routes: Routes = [
	{ path: 'login', component: LoginComponent, canActivate: [GuestGuard] },
	{ path: 'profile', component: ProfileComponent, canActivate: [AuthGuard] },
	{ path: 'posts', component: PostListComponent },
	{ path: 'posts/topics/:topicId', component: PostListComponent },
	{ path: 'posts/users/:userId', component: PostListComponent },
	{ path: 'posts/:id', component: PostDetailComponent },
	{ path: 'create-post', loadComponent: () => import('./components/create-post/create-post.component').then(m => m.CreatePostComponent), canActivate: [AuthGuard] },

	{ path: '', redirectTo: 'posts', pathMatch: 'full' }
];