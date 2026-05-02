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
import { ImageService } from '../../services/image.service';
import { firstValueFrom } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';

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
	loggedUser: User | null = null;
	avatarUrl: string = '';
	isLoadingUser = true;
	isOwnProfile = false;

	// PostList filter
	filterUsername: string | undefined;
	activeTab: string = 'posts';
	refreshTrigger: number = 1;

	// RxJS
	private destroy$ = new Subject<void>();
	private currentUserId: number | null = null;

	constructor(
		private userService: UserService,
		private authService: AuthService,
		private errorService: ErrorService,
		private route: ActivatedRoute,
		private router: Router,
		private imageService: ImageService,
		private dialog: MatDialog
	) { }

	ngOnInit(): void {
		// Get current user ID from AuthService
		this.authService.user$
			.pipe(takeUntil(this.destroy$))
			.subscribe(user => {
				this.loggedUser = user;
				this.currentUserId = user?.id || null;
				this.checkIfOwnProfile();
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
				next: (user) => {
					this.user = user;
					this.filterUsername = user.username;
					this.avatarUrl = 'api/v1/images/' + this.user.avatar + '?w=240&h=240';
					this.isLoadingUser = false;
					this.checkIfOwnProfile();
				}
			});
	}

	private checkIfOwnProfile(): void {
		if (!this.user || !this.currentUserId) {
			this.isOwnProfile = false;
			return;
		}

		const isAdmin = this.loggedUser?.roles.includes('ADMIN') ?? false;
		this.isOwnProfile = this.currentUserId === this.user.id || isAdmin;
	}

	goBack(): void {
		if (window.history.length > 1) {
			window.history.back();
		} else {
			this.router.navigate(['/posts']);
		}
	}

	editProfile(): void {
		if (this.user?.id) {
			this.router.navigate(['/users', this.user.id, 'edit']);
		}
	}

	onAvatarEditClick(): void {
		const dialogRef = this.dialog.open(ConfirmDialogComponent, {
			width: '400px',
			autoFocus: false,
			data: {
				title: 'Manage Avatar',
				message: 'Choose an action for your profile picture.',
				icon: 'photo_camera',
				confirmText: 'Upload avatar',
				color: 'primary',
				showSecondaryAction: !!this.user?.avatar,
				secondaryActionText: 'Delete avatar',
				secondaryActionColor: 'danger'
			}
		});

		dialogRef.afterClosed().subscribe(result => {
			if (result === true) {
				// Upload
				const fileInput = document.querySelector('#avatarFileInput') as HTMLInputElement;
				fileInput?.click();
			} else if (result === 'secondary') {
				// Delete
				this.deleteAvatar();
			}
		});
	}

	async onImageSelected(event: Event): Promise<void> {
		const input = event.target as HTMLInputElement;

		if (!input.files || input.files.length === 0) return;

		const file = input.files[0];

		// Validate file type
		if (!file.type.startsWith('image/')) {
			this.errorService.setError(400, 'Only image files are allowed');
			return;
		}

		if (!file.type.includes('png') && !file.type.includes('jpeg')) {
			this.errorService.setError(400, 'Only PNG and JPG images are allowed');
			return;
		}

		// 1. Upload image
		const uploadResponse = await firstValueFrom(
			this.imageService.uploadImages([file])
		);

		if (!uploadResponse || uploadResponse.length === 0) return;

		const imageId = uploadResponse[0];

		// 2. Update user profile with new avatar
		this.updateProfileImage(imageId);
	}

	private updateProfileImage(imageId: string): void {
		if (!this.user) return;

		this.userService.setAvatar(this.user.id, parseInt(imageId, 10)).subscribe({
			next: (user) => {
				this.user = user;
				this.avatarUrl = 'api/v1/images/' + user.avatar + '?w=240&h=240';
				this.authService.checkAuth().subscribe();
				this.refreshTrigger++;
			}
		});
	}

	deleteAvatar(): void {
		if (!this.user) return;
		this.userService.deleteAvatar(this.user.id).subscribe({
			next: () => {
				this.user!.avatar = undefined;
				this.avatarUrl = '';
				this.authService.checkAuth().subscribe();
				this.refreshTrigger++;
			}
		});
	}
}