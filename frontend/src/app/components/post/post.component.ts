import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { Post } from '../../models/post.model';
import { AuthService } from '../../services/auth.service';
import { map, Observable, take } from 'rxjs';
import { PostService } from '../../services/post.service';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmDialogComponent, ConfirmDialogData } from '../confirm-dialog/confirm-dialog.component';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
	selector: 'app-post',
	standalone: true,
	templateUrl: './post.component.html',
	styleUrls: ['./post.component.scss'],
	imports: [CommonModule, DatePipe]
})
export class PostComponent implements OnInit {
	@Output() postDeleted = new EventEmitter<void>();
	@Output() removeUnlikedPost = new EventEmitter<number | undefined>();
	@Input() post!: Post;
	@Input() mode: 'list' | 'detail' = 'list';

	avatarUrl: string = '';
	currentImageIndex = 0;
	imageCount = 0;
	currentImage: string = '';
	hasUserLiked = false;
	currentUserId: number | undefined = undefined;
	isOwner$!: Observable<boolean>;

	constructor(
		private postService: PostService,
		private authService: AuthService,
		private router: Router,
		private dialog: MatDialog,
		private snackBar: MatSnackBar
	) { }

	ngOnInit(): void {
		this.avatarUrl = 'api/v1/images/' + this.post.author?.avatar + '?w=240&h=240';
		this.isOwner$ = this.authService.user$.pipe(
			map(user => {
				this.currentUserId = user?.id;
				return user ? user.id === this.post.author?.id || user.roles.includes('ADMIN') : false;
			})
		);
		this.hasUserLiked = !!this.post.hasUserLiked;
		this.imageCount = this.post.images?.length || 0;
		if (this.post.topic?.id) {
			this.updateCurrentImage();
		}
	}

	nextImage(): void {
		if (this.imageCount > 0) {
			this.currentImageIndex = this.currentImageIndex + 1;
			if (this.currentImageIndex > this.imageCount - 1) this.currentImageIndex = this.imageCount - 1;
			this.updateCurrentImage();
		}
	}

	prevImage(): void {
		if (this.imageCount > 0) {
			this.currentImageIndex = this.currentImageIndex - 1;
			if (this.currentImageIndex < 1) this.currentImageIndex = 0;
			this.updateCurrentImage();
		}
	}

	private updateCurrentImage(): void {
		this.currentImage =
			'api/v1/images/' + (this.post.images?.[this.currentImageIndex] ?? '');
	}

	goToTopic(): void {
		if (this.post.topic?.id) {
			this.router.navigate(['/topics', this.post.topic.id], {
				state: { name: this.post.topic.name }
			});
		}
	}

	goToUser(): void {
		if (this.post.author?.id) {
			this.router.navigate(['/users', this.post.author.id], {
				state: { name: this.post.author.username }
			});
		}
	}

	goToPost(): void {
		if (this.post.id && this.mode === 'list') {
			this.router.navigate(['/posts', this.post.id]);
		}
	}

	onPostClick(): void {
		if (this.mode === 'list') {
			this.goToPost();
		}
	}

	toggleLike(): void {
		this.authService.loggedIn$.pipe(take(1)).subscribe(loggedIn => {
			if (loggedIn) {
				if (this.post.id) {
					this.postService.toggleLike(this.post.id).subscribe({
						next: (updatedPost: Post) => {
							this.post = updatedPost;
							this.hasUserLiked = !!updatedPost.hasUserLiked;
							this.removeUnlikedPost.emit(this.currentUserId);
						}
					});
				}
			}
		});
	}

	goToComments(): void {
		if (this.post.id && this.mode === 'list') {
			this.router.navigate(['/posts', this.post.id], { fragment: 'comments' });
		}
	}

	goBack(): void {
		if (window.history.length > 1) {
			window.history.back();
		} else {
			this.router.navigate(['/posts']);
		}
	}

	editPost(): void {
		if (this.post.id) {
			this.router.navigate(['/posts', this.post.id, 'edit']);
		}
	}

	openDeleteDialog(): void {
		const dialogData: ConfirmDialogData = {
			title: 'Delete Post',
			message: 'Are you sure you want to delete this post?\nThis action cannot be undone.',
			detail: this.post.title,
			confirmText: 'Delete',
			color: 'danger'
		};

		const dialogRef = this.dialog.open(ConfirmDialogComponent, {
			width: '400px',
			data: dialogData,
			autoFocus: false
		});

		dialogRef.afterClosed().pipe(
		).subscribe(result => {
			if (result) {
				this.deletePost();
			}
		});
	}

	private deletePost(): void {
		if (!this.post.id) {
			return;
		}

		this.postService.delete(this.post.id).subscribe({
			next: () => {
				// Navigate back if in detail mode
				if (this.mode === 'detail') {
					this.router.navigate(['/posts']);
				} else {
					// In list mode, delete the post without reload
					this.postDeleted.emit();
				}
				this.snackBar.open('Post deleted successfully', 'Close', {
					duration: 3000
				});
			}
		});
	}
}