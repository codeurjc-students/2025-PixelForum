import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
	selector: 'app-login',
	standalone: true,
	imports: [CommonModule,
		FormsModule,
		MatFormFieldModule,
		MatInputModule,
		MatButtonModule,
		MatCardModule
	],
	templateUrl: './login.component.html',
	styleUrls: ['./login.component.scss']
})
export class LoginComponent {

	username = '';
	password = '';
	errorMessage = '';

	constructor(
		private authService: AuthService,
		private router: Router,
		private snackBar: MatSnackBar
	) { }

	onSubmit(): void {
		this.errorMessage = '';

		this.authService.login(this.username, this.password).subscribe({
			next: () => {
				this.authService.checkAuth().subscribe(() => {
					this.snackBar.open('Login succesfull', 'Close', {
						duration: 3000
					});
					this.router.navigate(['/']);
				});
			},
			error: err => {
				if (err.status === 401) {
					this.errorMessage = 'Invalid username or password';
				} else if (err.status === 0) {
					this.errorMessage = 'Unable to connect to the server';
				} else {
					this.errorMessage = 'Unexpected error. Please try again later';
				}
			}
		});
	}

}