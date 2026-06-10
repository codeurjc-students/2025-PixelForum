import { Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs/internal/Subject';
import { takeUntil } from 'rxjs/internal/operators/takeUntil';
import { concatMap, from, take, toArray } from 'rxjs';
import { Comment } from '../../models/comment.model';
import { CommentComponent } from '../comment/comment.component';
import { CommentService } from '../../services/comment.service';
import { AuthService } from '../../services/auth.service';
import { PageResponse } from '../../models/pageResponse.model';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';

@Component({
	selector: 'app-comment-list',
	standalone: true,
	imports: [CommonModule, FormsModule, CommentComponent, RouterLink],
	templateUrl: './comment-list.component.html',
	styleUrls: ['./comment-list.component.scss']
})
export class CommentListComponent implements OnInit, OnChanges, OnDestroy {
	// ── Context ───────────────────────────────────────────────────────────────
	@Input() postId?: number;           // required when mode = 'post'
	@Input() userId?: number;           // required when mode = 'user' | 'liked'
	@Input() mode: 'post' | 'user' | 'liked' = 'post';

	// ── Presentation ─────────────────────────────────────────────────────────
	@Input() showForm = true;
	@Input() emptyMessage = 'No comments yet. Be the first!';
	@Input() refreshTrigger!: number;

	@Output() commentCountChanged = new EventEmitter<number>();

	comments: Comment[] = [];
	isLoading = true;
	isLoadingMore = false;
	isSubmitting = false;

	currentPage = 0;
	totalPages = 0;
	hasMorePages = false;
	totalElements = 0;
	readonly pageSize = 10;

	newCommentContent = '';
	isLoggedIn = false;

	private destroy$ = new Subject<void>();

	constructor(
		private commentService: CommentService,
		private authService: AuthService,
		private snackBar: MatSnackBar
	) { }

	ngOnInit(): void {
		this.authService.loggedIn$.pipe(takeUntil(this.destroy$)).subscribe(loggedIn => this.isLoggedIn = loggedIn);
		this.loadComments();
	}

	ngOnChanges(): void {
		if (this.refreshTrigger !== undefined) this.loadComments();
		const hasContext = this.mode === 'post' ? !!this.postId : !!this.userId;
		if (hasContext) this.loadComments();
	}

	ngOnDestroy(): void {
		this.destroy$.next();
		this.destroy$.complete();
	}

	loadComments(): void {
		this.isLoading = true;
		this.currentPage = 0;
		this.comments = [];
		this.fetchPage(true);
	}

	loadMoreComments(): void {
		if (!this.hasMorePages || this.isLoadingMore) return;
		this.isLoadingMore = true;
		this.currentPage++;
		this.fetchPage(false);
	}

	private getCommentsRequest(page: number) {
		return this.mode === 'user'
			? this.commentService.getUserComments(this.userId!, page, this.pageSize)
			: this.mode === 'liked'
				? this.commentService.getLikedComments(this.userId!, page, this.pageSize)
				: this.commentService.getPostComments(this.postId!, page, this.pageSize);
	}

	private fetchPage(isInitial: boolean): void {
		this.getCommentsRequest(this.currentPage).pipe(takeUntil(this.destroy$)).subscribe({
			next: (response: PageResponse<Comment>) => {
				if (isInitial) {
					this.comments = response.content;
					this.isLoading = false;
				} else {
					this.comments.push(...response.content);
					this.isLoadingMore = false;
				}
				this.totalPages = response.totalPages;
				this.totalElements = response.totalElements;
				this.hasMorePages = !response.last;
			}
		});
	}

	private reloadLoadedPages(): void {
		const pagesToFetch = Array.from({ length: this.currentPage + 1 }, (_, i) => i);

		from(pagesToFetch).pipe(
			concatMap(page => this.getCommentsRequest(page)),
			toArray(),
			takeUntil(this.destroy$)
		).subscribe({
			next: (responses: PageResponse<Comment>[]) => {
				this.comments = responses.flatMap(r => r.content);
				const last = responses[responses.length - 1];
				this.totalPages = last.totalPages;
				this.totalElements = last.totalElements;
				this.hasMorePages = !last.last;
			}
		});
	}

	submitComment(): void {
		if (!this.newCommentContent.trim() || this.isSubmitting || !this.postId) return;

		this.authService.loggedIn$.pipe(take(1)).subscribe(loggedIn => {
			if (!loggedIn) return;

			this.isSubmitting = true;
			this.commentService.create(this.postId!, { content: this.newCommentContent.trim() }).subscribe({
				next: (created: Comment) => {
					this.newCommentContent = '';
					this.isSubmitting = false;
					this.commentCountChanged.emit(+1);
					this.snackBar.open('Comment posted', 'Close', { duration: 3000 });
					this.reloadLoadedPages();
				},
				error: () => {
					this.isSubmitting = false;
				}
			});
		});
	}

	onCommentDeleted(commentId: number): void {
		this.comments = this.comments.filter(c => c.id !== commentId);
		this.totalElements = Math.max(0, this.totalElements - 1);
		this.commentCountChanged.emit(-1);
		this.reloadLoadedPages();
	}

	onCommentUpdated(updated: Comment): void {
		const index = this.comments.findIndex(c => c.id === updated.id);
		if (index !== -1) {
			this.comments[index] = updated;
		}
	}

	onCommentUnliked(commentId: number): void {
		if (this.mode === 'liked') {
			this.loadComments();
		}
	}
}