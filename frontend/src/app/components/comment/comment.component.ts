import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Comment } from '../../models/comment.model';
import { AuthService } from '../../services/auth.service';
import { CommentService } from '../../services/comment.service';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ConfirmDialogComponent, ConfirmDialogData } from '../confirm-dialog/confirm-dialog.component';
import { map, Observable, take } from 'rxjs';
import { Router } from '@angular/router';

@Component({
	selector: 'app-comment',
	standalone: true,
	imports: [CommonModule, DatePipe, FormsModule],
	templateUrl: './comment.component.html',
	styleUrls: ['./comment.component.scss']
})
export class CommentComponent implements OnInit {
	@Input() comment!: Comment;
	@Input() postId?: number;
	@Input() readonly = false;
	@Output() commentDeleted = new EventEmitter<number>();
	@Output() commentUpdated = new EventEmitter<Comment>();
	@Output() commentUnliked = new EventEmitter<number>();

	isOwner$!: Observable<boolean>;
	hasUserLiked = false;

	isEditing = false;
	editContent = '';

	avatarUrl: string = '';

	constructor(
		private authService: AuthService,
		private commentService: CommentService,
		private router: Router,
		private dialog: MatDialog,
		private snackBar: MatSnackBar
	) { }

	ngOnInit(): void {
		this.hasUserLiked = !!this.comment.hasUserLiked;
		this.avatarUrl = 'api/v1/images/' + this.comment.author?.avatar + '?w=240&h=240';
		this.isOwner$ = this.authService.user$.pipe(
			map(user =>
				user ? user.id === this.comment.author?.id || user.roles.includes('ADMIN') : false
			)
		);
	}

	goToUser(): void {
		if (this.comment.author?.id) {
			this.router.navigate(['/users', this.comment.author.id], {
				state: { name: this.comment.author.username }
			});
		}
	}

	goToPost(): void {
		const pid = this.postId ?? this.comment.post;
		if (pid) this.router.navigate(['/posts', pid], { state: { scrollToComments: true } });
	}

	toggleLike(): void {
		const pid = this.postId ?? this.comment.post;
		if (!pid) return;
		this.authService.loggedIn$.pipe(take(1)).subscribe(loggedIn => {
			if (!loggedIn || !this.comment.id) return;
			this.commentService.toggleLike(pid, this.comment.id).subscribe({
				next: (updated: Comment) => {
					const wasLiked = this.hasUserLiked;
					this.comment = updated;
					this.hasUserLiked = !!updated.hasUserLiked;
					if (wasLiked && !this.hasUserLiked) {
						this.commentUnliked.emit(this.comment.id);
					}
				}
			});
		});
	}

	startEdit(): void {
		this.editContent = this.comment.content ?? '';
		this.isEditing = true;
	}

	cancelEdit(): void {
		this.isEditing = false;
		this.editContent = '';
	}

	saveEdit(): void {
		const pid = this.postId ?? this.comment.post;
		if (!this.editContent.trim() || !this.comment.id || !pid) return;
		this.commentService.update(pid, this.comment.id, { content: this.editContent }).subscribe({
			next: (updated: Comment) => {
				this.comment = updated;
				this.isEditing = false;
				this.commentUpdated.emit(updated);
				this.snackBar.open('Comment updated', 'Close', { duration: 3000 });
			}
		});
	}

	openDeleteDialog(): void {
		const dialogData: ConfirmDialogData = {
			title: 'Delete Comment',
			message: 'Are you sure you want to delete this comment?\nThis action cannot be undone.',
			detail: this.comment.content,
			confirmText: 'Delete',
			color: 'danger'
		};

		const dialogRef = this.dialog.open(ConfirmDialogComponent, {
			width: '400px',
			data: dialogData,
			autoFocus: false
		});

		dialogRef.afterClosed().subscribe(result => {
			if (result) this.deleteComment();
		});
	}

	private deleteComment(): void {
		const pid = this.postId ?? this.comment.post;
		if (!this.comment.id || !pid) return;
		this.commentService.delete(pid, this.comment.id).subscribe({
			next: () => {
				this.commentDeleted.emit(this.comment.id);
				this.snackBar.open('Comment deleted', 'Close', { duration: 3000 });
			}
		});
	}
}