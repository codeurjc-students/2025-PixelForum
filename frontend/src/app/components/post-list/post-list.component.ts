import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Post } from '../../models/post.model';
import { PostComponent } from '../post/post.component';
import { PostService } from '../../services/post.service';
import { TopicService } from '../../services/topic.service';
import { Topic } from '../../models/topic.model';
import { UserService } from '../../services/user.service';
import { User } from '../../models/user.model';

type FilterType = 'all' | 'topic' | 'user';

@Component({
	selector: 'app-post-list',
	standalone: true,
	imports: [CommonModule, PostComponent],
	templateUrl: './post-list.component.html',
	styleUrls: ['./post-list.component.scss']
})
export class PostListComponent implements OnInit {
	posts: Post[] = [];
	loading = true;
	error: string | null = null;

	// Filter configuration
	filterType: FilterType = 'all';
	filterId: number | null = null;
	filterName: string = 'All Posts';

	constructor(
		private postService: PostService,
		private topicService: TopicService,
		private userService: UserService,
		private route: ActivatedRoute,
		private router: Router
	) { }

	ngOnInit(): void {
		this.route.params.subscribe(params => {
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
				this.filterName = 'Last Posts';
			}

			this.loadPosts();
		});
	}

	loadUserName(): void {
		if (this.filterId) {
			this.userService.getById(this.filterId).subscribe({
				next: (user: User) => {
					this.filterName = user.username;
				},
				error: (err) => {
					console.error('Error loading user:', err);
					this.filterName = 'User Posts';
				}
			});
		}
	}

	loadTopicName(): void {
		if (this.filterId) {
			this.topicService.getById(this.filterId).subscribe({
				next: (topic: Topic) => {
					this.filterName = topic.name;
				},
				error: (err) => {
					console.error('Error loading topic:', err);
					this.filterName = 'Topic Posts';
				}
			});
		}
	}

	loadPosts(): void {
		this.loading = true;
		this.error = null;

		this.postService.getAll().subscribe({
			next: data => {
				let filtered = data;

				// Apply filters
				if (this.filterType === 'topic' && this.filterId) {
					filtered = filtered.filter(post => post.topic?.id === this.filterId);
				} else if (this.filterType === 'user' && this.filterId) {
					filtered = filtered.filter(post => post.author?.id === this.filterId);
				}

				// Sort by date
				this.posts = filtered.sort((a, b) =>
					new Date(b.createdAt || '').getTime() -
					new Date(a.createdAt || '').getTime()
				);

				this.loading = false;
			},
			error: err => {
				console.error('Error loading posts', err);
				this.error = 'Error loading posts';
				this.loading = false;
			}
		});
	}

	goBack(): void {
		if (window.history.length > 1) {
			window.history.back();
		} else {
			this.router.navigate(['/posts']);
		}
	}
}