import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs/internal/Subject';
import { takeUntil } from 'rxjs/internal/operators/takeUntil';

import { Post } from '../../models/post.model';
import { PostComponent } from '../post/post.component';
import { PostService } from '../../services/post.service';
import { TopicService } from '../../services/topic.service';
import { Topic } from '../../models/topic.model';
import { UserService } from '../../services/user.service';
import { User } from '../../models/user.model';
import { PageResponse } from '../../models/pageResponse.model';
import { ErrorService } from '../../services/error.service';

@Component({
	selector: 'app-post-list',
	standalone: true,
	imports: [CommonModule, PostComponent],
	templateUrl: './post-list.component.html',
	styleUrls: ['./post-list.component.scss']
})
export class PostListComponent implements OnInit, OnDestroy {
	// State
	posts: Post[] = [];
	isLoading = true;
	isLoadingMore = false;

	// Pagination
	currentPage = 0;
	pageSize = 10;
	totalPages = 0;
	hasMorePages = false;
	totalElements = 0;

	// Filters
	filterType: 'all' | 'topic' | 'user' = 'all';
	filterId: number | null = null;
	filterName: string = '';
	filterUsername?: string;
	filterTopic?: string;

	// RxJS
	private destroy$ = new Subject<void>();

	constructor(
		private postService: PostService,
		private topicService: TopicService,
		private userService: UserService,
		private errorService: ErrorService,
		private route: ActivatedRoute,
		private router: Router
	) { }

	ngOnInit(): void {
		this.route.params
			.pipe(takeUntil(this.destroy$))
			.subscribe(params => {
				this.resetPagination();
				this.resetFilters();

				if (params['topicId']) {
					this.filterType = 'topic';
					this.filterId = parseInt(params['topicId'], 10);
					this.loadTopicName();
				} else if (params['userId']) {
					this.filterType = 'user';
					this.filterId = parseInt(params['userId'], 10);
					this.loadUserName();
				} else {
					this.filterType = 'all';
					this.filterName = 'Latest Posts';
					this.loadPosts();
				}
			});
	}

	ngOnDestroy(): void {
		this.destroy$.next();
		this.destroy$.complete();
	}

	loadTopicName(): void {
		if (this.filterId) {
			this.topicService.getById(this.filterId).subscribe({
				next: (topic: Topic) => {
					this.filterName = topic.name;
					this.filterTopic = topic.name;
					this.loadPosts();
				}
			});
		} else {
			this.errorService.setError(400, "Bad Request");
			this.router.navigate(['/error']);
		}
	}

	loadUserName(): void {
		if (this.filterId) {
			this.userService.getById(this.filterId).subscribe({
				next: (user: User) => {
					this.filterName = user.username;
					this.filterUsername = user.username;
					this.loadPosts();
				}
			});
		} else {
			this.errorService.setError(400, "Bad Request");
			this.router.navigate(['/error']);
		}
	}

	loadPosts(): void {
		this.isLoading = true;
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
		this.postService.getPosts(
			this.currentPage,
			this.pageSize,
			undefined,
			this.filterUsername,
			this.filterTopic,
			'createdAt',
			'desc'
		)
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

	private resetPagination(): void {
		this.currentPage = 0;
		this.totalPages = 0;
		this.hasMorePages = false;
		this.totalElements = 0;
		this.posts = [];
	}

	private resetFilters(): void {
		this.filterUsername = undefined;
		this.filterTopic = undefined;
		this.filterType = 'all';
		this.filterId = null;
		this.filterName = '';
	}

	goBack(): void {
		if (window.history.length > 1) {
			window.history.back();
		} else {
			this.router.navigate(['/posts']);
		}
	}
}