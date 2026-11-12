import { Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Post } from '../../models/post.model';
import { PostComponent } from '../post/post.component';
import { PostService } from '../../services/post.service';
import { PageResponse } from '../../models/pageResponse.model';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { concatMap, distinctUntilChanged, from, map, Subject, Subscription, takeUntil, toArray } from 'rxjs';

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
	private isReloading = false;

	// Pagination
	currentPage = 0;
	totalPages = 0;
	hasMorePages = false;
	totalElements = 0;

	// RxJS
	private destroy$ = new Subject<void>();
	private loadSub?: Subscription;

	constructor(
		private postService: PostService,
		private userService: UserService,
		private authService: AuthService
	) { }

	ngOnInit(): void {
		this.authService.user$
			.pipe(
				map(user => user?.id),
				distinctUntilChanged(),
				takeUntil(this.destroy$)
			)
			.subscribe(() => this.loadPosts());
	}

	ngOnChanges(changes: SimpleChanges): void {
		const keys = ['filterUsername', 'filterTopic', 'sortBy', 'sortOrder', 'pageSize', 'likedByUserId', 'refreshTrigger'];
		if (keys.some(key => changes[key] && !changes[key].firstChange)) this.loadPosts();
	}

	ngOnDestroy(): void {
		this.destroy$.next();
		this.destroy$.complete();
	}

	loadPosts(): void {
		this.loadSub?.unsubscribe();
		this.isLoading = true;
		this.isLoadingMore = false;
		this.isReloading = false;
		this.currentPage = 0;
		this.posts = [];
		this.fetchPosts(true);
	}

	loadMorePosts(): void {
		if (!this.hasMorePages || this.isLoading || this.isLoadingMore || this.isReloading) return;

		this.isLoadingMore = true;
		this.currentPage++;
		this.fetchPosts(false);
	}

	private getPostsRequest(page: number) {
		return this.likedByUserId
			? this.userService.getLikedPosts(this.likedByUserId, page, this.pageSize)
			: this.postService.getPosts(page, this.pageSize, undefined, this.filterUsername, this.filterTopic, this.sortBy, this.sortOrder);
	}

	private fetchPosts(isInitialLoad: boolean): void {
		this.loadSub?.unsubscribe();
		this.loadSub = this.getPostsRequest(this.currentPage)
			.pipe(takeUntil(this.destroy$))
			.subscribe({
				next: (response: PageResponse<Post>) => {
					if (isInitialLoad) {
						this.posts = response.content;
						this.isLoading = false;
					} else {
						this.posts.push(...response.content);
						this.isLoadingMore = false;
					}
					this.updatePaginationInfo(response);
				},
				error: () => {
					if (isInitialLoad) {
						this.isLoading = false;
					} else {
						this.isLoadingMore = false;
						this.currentPage--;
					}
				}
			});
	}

	private reloadLoadedPages(): void {
		this.loadSub?.unsubscribe();
		this.isReloading = true;
		const pagesToFetch = Array.from({ length: this.currentPage + 1 }, (_, i) => i);

		this.loadSub = from(pagesToFetch).pipe(
			concatMap(page => this.getPostsRequest(page)),
			toArray(),
			takeUntil(this.destroy$)
		).subscribe({
			next: (responses: PageResponse<Post>[]) => {
				this.posts = responses.flatMap(r => r.content);
				this.updatePaginationInfo(responses[responses.length - 1]);
				this.endReload();
			},
			error: () => {
				this.endReload();
			}
		});
	}

	private endReload(): void {
		this.isReloading = false;
		this.isLoading = false;
		this.isLoadingMore = false;
	}

	private updatePaginationInfo(response: PageResponse<Post>): void {
		this.totalPages = response.totalPages;
		this.totalElements = response.totalElements;
		this.hasMorePages = !response.last;
	}

	removePost(): void {
		this.reloadLoadedPages();
	}

	removeUnlikedPost(id: number | undefined): void {
		if (!this.likedByUserId || !id || id != this.likedByUserId) return;
		this.reloadLoadedPages();
	}
}