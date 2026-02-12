import { Component } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
	selector: 'app-header',
    standalone: true,
	imports: [CommonModule, RouterModule],
	templateUrl: './header.component.html',
	styleUrls: ['./header.component.scss']
})
export class HeaderComponent {
	constructor(public authService: AuthService) {}

	onLogout() {
		this.authService.logout().subscribe();
	}
}
