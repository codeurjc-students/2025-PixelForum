import { AfterViewChecked, Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
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
import { normalizeMultiline, validateMultiline } from '../../utils/text-rules';

@Component({
	selector: 'app-comment',
	standalone: true,
	imports: [CommonModule, DatePipe, FormsModule],
	templateUrl: './comment.component.html',
	styleUrls: ['./comment.component.scss']
})
export class CommentComponent implements OnInit, AfterViewChecked {
	@Input() comment!: Comment;
	@Input() postId?: number;
	@Input() readonly = false;
	@Output() commentDeleted = new EventEmitter<number>();
	@Output() commentUpdated = new EventEmitter<Comment>();
	@Output() commentUnliked = new EventEmitter<number>();
	@ViewChild('contentInput') contentInput?: ElementRef<HTMLElement>;

	isOwner$!: Observable<boolean>;
	hasUserLiked = false;

	isExpanded = false;
	isClamped = false;
	isEditing = false;
	editContent = '';
	readonly commentRules = { label: 'The comment', maxLength: 2000, maxLines: 30 };

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

	ngAfterViewChecked(): void {
		const elem = this.contentInput?.nativeElement;
		if (!elem || this.isExpanded) return;
		const clamped = elem.scrollHeight > elem.clientHeight;
		if (clamped !== this.isClamped) {
			// Avoid ExpressionChangedAfterItHasBeenChecked
			Promise.resolve().then(() => this.isClamped = clamped);
		}
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

	toggleExpand(): void {
		this.isExpanded = !this.isExpanded;
	}

	startEdit(): void {
		this.editContent = this.comment.content ?? '';
		this.isEditing = true;
	}

	cancelEdit(): void {
		this.isEditing = false;
		this.editContent = '';
	}

	get commentValidationError(): string | null {
		if (!this.editContent) return null;
		return validateMultiline(this.editContent, this.commentRules);
	}

	saveEdit(): void {
		const pid = this.postId ?? this.comment.post;
		if (!this.comment.id || !pid || this.commentValidationError || !this.editContent) return;

		const content = normalizeMultiline(this.editContent);
		this.commentService.update(pid, this.comment.id, { content }).subscribe({
			next: (updated: Comment) => {
				this.comment = updated;
				this.isEditing = false;
				this.editContent = '';
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