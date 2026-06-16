import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';
import { Post } from '../../models/post.model';
import { PostService } from '../../services/post.service';
import { PostComponent } from '../post/post.component';
import { CommentListComponent } from '../comment-list/comment-list.component';

@Component({
    selector: 'app-post-detail',
    standalone: true,
    imports: [CommonModule, PostComponent, CommentListComponent],
    templateUrl: './post-detail.component.html',
    styleUrls: ['./post-detail.component.scss']
})
export class PostDetailComponent implements OnInit {
    post: Post | null = null;
    loading = true;
    scrollToComments = false;

    constructor(
        private postService: PostService,
        private route: ActivatedRoute,
        private location: Location,
        private router: Router
    ) {
        const nav = this.router.getCurrentNavigation();
        this.scrollToComments = nav?.extras?.state?.['scrollToComments'] ?? false;
        if (this.scrollToComments) {
            history.replaceState({}, '');
        }
    }

    ngOnInit(): void {
        this.route.params.subscribe(params => {
            const id = params['id'];
            if (id) {
                this.loadPost(id);
            } else {
                this.loading = false;
            }
        });
    }

    loadPost(id: number): void {
        this.loading = true;

        this.postService.getById(id).subscribe({
            next: data => {
                this.post = data;
                this.loading = false;

                if (this.scrollToComments) {
                    setTimeout(() => {
                        const element = document.getElementById('comments');
                        element?.scrollIntoView({ behavior: 'smooth' });
                    }, 100);
                }
            }
        });
    }

    onCommentCountChanged(delta: number): void {
        if (this.post) {
            this.post = {
                ...this.post,
                commentsCount: Math.max(0, (this.post.commentsCount ?? 0) + delta)
            };
        }
    }

    goBack(): void {
        if (window.history.length > 1) {
            this.location.back();
        } else {
            this.router.navigate(['/posts']);
        }
    }
}