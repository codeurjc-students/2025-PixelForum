import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs/internal/Subject';
import { takeUntil } from 'rxjs/internal/operators/takeUntil';

import { User } from '../../models/user.model';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { ErrorService } from '../../services/error.service';
import { PostListComponent } from '../post-list/post-list.component';

@Component({
	selector: 'app-profile',
	standalone: true,
	imports: [CommonModule, PostListComponent],
	templateUrl: './profile.component.html',
	styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit, OnDestroy {
	// User data
	user: User | null = null;
	isLoadingUser = true;
	isOwnProfile = false;

	// PostList filter
	filterUsername: string | undefined;
	activeTab: string = 'posts';

	// RxJS
	private destroy$ = new Subject<void>();
	private currentUserId: number | null = null;

	constructor(
		private userService: UserService,
		private authService: AuthService,
		private errorService: ErrorService,
		private route: ActivatedRoute,
		private router: Router
	) { }

	ngOnInit(): void {
		// Get current user ID from AuthService
		this.authService.user$
			.pipe(takeUntil(this.destroy$))
			.subscribe(user => {
				this.currentUserId = user?.id || null;
			});

		// Load user profile
		this.route.params
			.pipe(takeUntil(this.destroy$))
			.subscribe(params => {
				if (params['userId']) {
					this.loadUserProfile(parseInt(params['userId'], 10));
				} else {
					this.errorService.setError(400, "Bad Request");
					this.router.navigate(['/error']);
				}
			});
	}

	ngOnDestroy(): void {
		this.destroy$.next();
		this.destroy$.complete();
	}

	loadUserProfile(userId: number): void {
		this.isLoadingUser = true;
		this.userService.getById(userId)
			.pipe(takeUntil(this.destroy$))
			.subscribe({
				next: (user: User) => {
					this.user = user;
					this.filterUsername = user.username;
					this.checkIfOwnProfile(userId);
					this.isLoadingUser = false;
				}
			});
	}

	private checkIfOwnProfile(userId: number): void {
		this.isOwnProfile = this.currentUserId === userId;
	}

	goBack(): void {
		if (window.history.length > 1) {
			window.history.back();
		} else {
			this.router.navigate(['/posts']);
		}
	}

	editProfile(): void {
		// TODO
		this.router.navigate(['/profile/edit']);
	}
}