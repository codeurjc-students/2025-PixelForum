import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject } from 'rxjs/internal/Subject';
import { takeUntil } from 'rxjs/internal/operators/takeUntil';
import { Post } from '../../models/post.model';
import { PostComponent } from '../post/post.component';
import { PostService } from '../../services/post.service';
import { PageResponse } from '../../models/pageResponse.model';
import { UserService } from '../../services/user.service';

@Component({
	selector: 'app-post-list',
	standalone: true,
	imports: [CommonModule, PostComponent],
	templateUrl: './post-list.component.html',
	styleUrls: ['./post-list.component.scss']
})
export class PostListComponent implements OnInit, OnChanges, OnDestroy {
	// Input filters
	@Input() filterUsername?: string;
	@Input() filterTopic?: string;
	@Input() sortBy: string = 'createdAt';
	@Input() sortOrder: 'asc' | 'desc' = 'desc';
	@Input() pageSize: number = 10;
	@Input() refreshTrigger!: number;
	@Input() likedByUserId?: number;

	// State
	posts: Post[] = [];
	isLoading = true;
	isLoadingMore = false;

	// Pagination
	currentPage = 0;
	totalPages = 0;
	hasMorePages = false;
	totalElements = 0;

	// RxJS
	private destroy$ = new Subject<void>();

	constructor(
		private postService: PostService,
		private userService: UserService
	) { }

	ngOnInit(): void {
		this.loadPosts();
	}

	ngOnChanges() {
		if (this.refreshTrigger !== undefined) this.loadPosts();
	}

	ngOnDestroy(): void {
		this.destroy$.next();
		this.destroy$.complete();
	}

	loadPosts(): void {
		this.isLoading = true;
		this.currentPage = 0;
		this.posts = [];
		this.fetchPosts(true);
	}

	loadMorePosts(): void {
		if (!this.hasMorePages || this.isLoadingMore) {
			return;
		}

		this.isLoadingMore = true;
		this.currentPage++;
		this.fetchPosts(false);
	}

	fetchPosts(isInitialLoad: boolean): void {
		const request$ = this.likedByUserId
			? this.userService.getLikedPosts(
				this.likedByUserId,
				this.currentPage,
				this.pageSize
			)
			: this.postService.getPosts(
				this.currentPage,
				this.pageSize,
				undefined,
				this.filterUsername,
				this.filterTopic,
				this.sortBy,
				this.sortOrder
			);

		request$
			.pipe(takeUntil(this.destroy$))
			.subscribe({
				next: (response: PageResponse<Post>) => {
					if (isInitialLoad) {
						this.posts = response.content;
						this.updatePaginationInfo(response);
						this.isLoading = false;
					} else {
						this.posts.push(...response.content);
						this.updatePaginationInfo(response);
						this.isLoadingMore = false;
					}
				}
			});
	}

	private updatePaginationInfo(response: PageResponse<Post>): void {
		this.totalPages = response.totalPages;
		this.totalElements = response.totalElements;
		this.hasMorePages = !response.last;
	}

	removePost(): void {
		this.loadPosts();
	}

	removeUnlikedPost(id: number | undefined): void {
		if (!this.likedByUserId || !id || id != this.likedByUserId) {
			return;
		}
		this.loadPosts();
	}
}