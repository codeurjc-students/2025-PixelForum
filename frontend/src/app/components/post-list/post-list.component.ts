import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Post } from '../../models/post.model';
import { PostComponent } from '../post/post.component';
import { PostService } from '../../service/post.service';

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

	constructor(private postService: PostService) {}

	ngOnInit(): void {
		this.loadPosts();
	}

	loadPosts(): void {
		/*this.loading = true;
		this.postService.getAll().subscribe(posts => this.posts = posts);*/
		this.postService.getAll().subscribe({
			next: data => {
				this.posts = data.sort((a, b) =>
					new Date(b.createdAt || '').getTime() -
					new Date(a.createdAt || '').getTime()
				);
				this.loading = false;
			},
			error: err => {
				console.error('Error loading posts', err);
				this.error = 'Error cargando los posts.';
				this.loading = false;
			}
		});
	}
}
