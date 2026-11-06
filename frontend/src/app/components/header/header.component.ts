import { Component } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { User } from '../../models/user.model';
import { Topic } from '../../models/topic.model';
import { TopicService } from '../../services/topic.service';

@Component({
	selector: 'app-header',
	standalone: true,
	imports: [CommonModule, RouterLink],
	templateUrl: './header.component.html',
	styleUrls: ['./header.component.scss']
})
export class HeaderComponent {
	user: User | null = null;
	avatarUrl: string = '';
	topics: Topic[] = [];
	results: Topic[] = [];

	constructor(public authService: AuthService, private router: Router, private topicService: TopicService) { }

	ngOnInit() {
		this.authService.checkAuth().subscribe();
		this.topicService.getAll().subscribe(topics => this.topics = topics);

		this.authService.user$.subscribe(user => {
			this.user = user;
			this.avatarUrl = 'api/v1/images/' + this.user?.avatar + '?w=240&h=240';
		});
	}

	onSearch(query: string) {
		const q = query.trim().toLowerCase();
		this.results = q ? this.topics.filter(t => t.name.toLowerCase().includes(q)).slice(0, 8) : [];
	}

	goToTopic(topic: Topic, input: HTMLInputElement) {
		this.results = [];
		input.value = '';
		this.router.navigate(['/topics', topic.id]);
	}

	onLogout() {
		this.authService.logout().subscribe({
			next: () => {
				this.authService.checkAuth().subscribe(() => {
					this.router.navigate(['/']);
				});
			}
		});
	}
}