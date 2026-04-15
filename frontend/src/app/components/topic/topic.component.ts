import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs/internal/Subject';
import { takeUntil } from 'rxjs/internal/operators/takeUntil';
import { Topic } from '../../models/topic.model';
import { TopicService } from '../../services/topic.service';
import { ErrorService } from '../../services/error.service';
import { PostListComponent } from '../post-list/post-list.component';

@Component({
    selector: 'app-topic',
    standalone: true,
    imports: [CommonModule, PostListComponent],
    templateUrl: './topic.component.html',
    styleUrls: ['./topic.component.scss']
})
export class TopicComponent implements OnInit, OnDestroy {
    // Topic data
    topic: Topic | null = null;
    isLoadingTopic = true;

    // PostList filter
    filterTopicName: string | undefined;

    // RxJS
    private destroy$ = new Subject<void>();

    constructor(
        private topicService: TopicService,
        private errorService: ErrorService,
        private route: ActivatedRoute,
        private router: Router
    ) { }

    ngOnInit(): void {
        // Load topic
        this.route.params
            .pipe(takeUntil(this.destroy$))
            .subscribe(params => {
                if (params['topicId']) {
                    this.loadTopic(parseInt(params['topicId'], 10));
                } else {
                    this.errorService.setError(400, "Bad Request");
                    this.router.navigate(['/error']);
                }
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }

    loadTopic(topicId: number): void {
        this.isLoadingTopic = true;
        this.topicService.getById(topicId)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: (topic: Topic) => {
                    this.topic = topic;
                    this.filterTopicName = topic.name;
                    this.isLoadingTopic = false;
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