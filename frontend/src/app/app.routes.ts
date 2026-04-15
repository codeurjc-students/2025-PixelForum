import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { AuthGuard } from './guards/auth.guard';
import { GuestGuard } from './guards/guest.guard';
import { ProfileComponent } from './components/profile/profile.component';
import { PostDetailComponent } from './components/post-detail/post-detail.component';
import { ErrorComponent } from './components/error/error.component';
import { CreatePostComponent } from './components/create-post/create-post.component';
import { TopicComponent } from './components/topic/topic.component';
import { PostPageComponent } from './components/post-page/post-page.component';

export const routes: Routes = [
	{ path: 'login', component: LoginComponent, canActivate: [GuestGuard] },
	{ path: 'posts', component: PostPageComponent },
	{ path: 'topics/:topicId', component: TopicComponent },
	{ path: 'users/:userId', component: ProfileComponent },
	{ path: 'posts/:id', component: PostDetailComponent },
	{ path: 'create-post', component: CreatePostComponent, canActivate: [AuthGuard] },
	{ path: 'posts/:id/edit', component: CreatePostComponent, canActivate: [AuthGuard] },

	{ path: 'error', component: ErrorComponent },

	{ path: '', redirectTo: 'posts', pathMatch: 'full' },
	{ path: '**', redirectTo: 'error' }
];