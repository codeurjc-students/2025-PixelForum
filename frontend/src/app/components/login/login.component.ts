import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
	selector: 'app-login',
	standalone: true,
	imports: [CommonModule, FormsModule],
	templateUrl: './login.component.html',
	styleUrls: ['./login.component.scss']
})
export class LoginComponent {

	username = '';
	password = '';
	errorMessage = '';

	constructor(
		private authService: AuthService,
		private router: Router
	) {}

	onSubmit(): void {
		this.errorMessage = '';

		this.authService.login(this.username, this.password).subscribe({
			next: () => {
				this.router.navigate(['/']);
			},
			error: () => {
				this.errorMessage = 'Usuario o contraseña incorrectos';
			}
		});
	}
}