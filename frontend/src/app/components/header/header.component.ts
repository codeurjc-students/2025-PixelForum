import { Component } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { User } from '../../models/user.model';

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

	constructor(public authService: AuthService, private router: Router) { }

	ngOnInit() {
		this.authService.checkAuth().subscribe();

		this.authService.user$.subscribe(user => {
			this.user = user;
			this.avatarUrl = 'api/v1/images/' + this.user?.avatar + '?w=240&h=240';
		});
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

	onCreatePost() {
		this.router.navigate(['/create-post']);
	}
}
