import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';
import { Post } from '../../models/post.model';
import { PostService } from '../../services/post.service';
import { PostComponent } from '../post/post.component';

@Component({
    selector: 'app-post-detail',
    standalone: true,
    imports: [CommonModule, PostComponent],
    templateUrl: './post-detail.component.html',
    styleUrls: ['./post-detail.component.scss']
})
export class PostDetailComponent implements OnInit {
    post: Post | null = null;
    loading = true;
    error: string | null = null;

    constructor(
        private postService: PostService,
        private route: ActivatedRoute,
        private location: Location,
        private router: Router
    ) { }

    ngOnInit(): void {
        this.route.params.subscribe(params => {
            const id = params['id'];
            if (id) {
                this.loadPost(id);
            } else {
                this.error = 'Post not found';
                this.loading = false;
            }
        });
    }

    loadPost(id: number): void {
        this.loading = true;
        this.error = null;

        this.postService.getById(id).subscribe({
            next: data => {
                this.post = data;
                this.loading = false;
            },
            error: err => {
                console.error('Error loading post', err);
                this.error = 'Post not found';
                this.loading = false;
            }
        });
    }

    goBack(): void {
        if (window.history.length > 1) {
            this.location.back();
        } else {
            this.router.navigate(['/posts']);
        }
    }
}