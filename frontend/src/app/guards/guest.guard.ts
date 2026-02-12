import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { catchError, map, of } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class GuestGuard implements CanActivate {

	constructor(
		private authService: AuthService,
		private router: Router
	) {}

	canActivate() {
		return this.authService.me().pipe(
			map(() => {
				this.router.navigate(['/posts']);
				return false;
			}),
			catchError(() => of(true))
		);
	}
}
