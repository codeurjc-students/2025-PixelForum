import { Component } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';

@Component({
	selector: 'app-header',
    standalone: true,
	imports: [CommonModule, RouterLink],
	templateUrl: './header.component.html',
	styleUrls: ['./header.component.scss']
})
export class HeaderComponent {
	constructor(public authService: AuthService, private router: Router) {}
	
	ngOnInit() {
		this.authService.checkAuth().subscribe();
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
