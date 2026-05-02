import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ErrorService } from '../services/error.service';
import { map, Observable } from 'rxjs';

@Injectable({
    providedIn: 'root'
})
export class OwnerGuard implements CanActivate {

    constructor(
        private authService: AuthService,
        private errorService: ErrorService,
        private router: Router
    ) { }

    canActivate(route: ActivatedRouteSnapshot): Observable<boolean> {
        const routeUserId = Number(route.paramMap.get('userId'));

        return this.authService.checkAuth().pipe(
            map(user => {
                if (!user) {
                    this.router.navigate(['/login']);
                    return false;
                }

                const isOwner = user.id === routeUserId;
                const isAdmin = user.roles.includes('ADMIN');

                if (isOwner || isAdmin) {
                    return true;
                }

                this.errorService.setError(403, "Forbbiden");
                this.router.navigate(['/error']);
                return false;
            })
        );
    }
}