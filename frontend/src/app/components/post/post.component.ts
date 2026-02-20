import { Component, Input, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { Post } from '../../models/post.model';
import { Topic } from '../../models/topic.model';
import { TopicService } from '../../services/topic.service';

@Component({
	selector: 'app-post',
	standalone: true,
	templateUrl: './post.component.html',
	styleUrls: ['./post.component.scss'],
	imports: [CommonModule, DatePipe]
})
export class PostComponent implements OnInit {
	@Input() post!: Post;
	@Input() mode: 'list' | 'detail' = 'list';

	currentImageIndex = 0;
	imageCount = 0;
	topicData: Topic | null = null;

	constructor(
		private router: Router,
	) { }

	ngOnInit(): void {
		this.imageCount = this.post.images?.length || 0;
		if (this.post.topic?.id) {
			this.loadTopicData();
		}
	}

	loadTopicData(): void {
		if (this.post.topic?.id) {
			this.topicData = this.post.topic;
		}
	}

	nextImage(): void {
		if (this.imageCount > 0) {
			this.currentImageIndex = this.currentImageIndex + 1;
			if (this.currentImageIndex > this.imageCount - 1) this.currentImageIndex = this.imageCount - 1;
		}
	}

	prevImage(): void {
		if (this.imageCount > 0) {
			this.currentImageIndex = this.currentImageIndex - 1;
			if (this.currentImageIndex < 1) this.currentImageIndex = 0;
		}
	}

	getCurrentImage(): string {
		if (this.post.images && this.post.images.length > 0) {
			return this.post.images[this.currentImageIndex];
		}
		return '';
	}

	goToTopic(): void {
		if (this.post.topic?.id) {
			this.router.navigate(['/posts/topics', this.post.topic.id], {
				state: { name: this.post.topic.name }
			});
		}
	}

	goToUser(): void {
		if (this.post.author?.id) {
			this.router.navigate(['/posts/users', this.post.author.id], {
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
		// TO DO: Like function
		console.log('Like post:', this.post.id);
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
}