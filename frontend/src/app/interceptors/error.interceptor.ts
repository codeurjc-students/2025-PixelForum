import { Injectable } from '@angular/core';
import {
    HttpEvent,
    HttpHandler,
    HttpInterceptor,
    HttpRequest,
    HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ErrorService } from '../services/error.service';

@Injectable({ providedIn: 'root' })
export class ErrorInterceptor implements HttpInterceptor {

    constructor(
        private router: Router,
        private authService: AuthService,
        private errorService: ErrorService
    ) { }

    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {

        return next.handle(req).pipe(
            catchError((error: HttpErrorResponse) => {

                if (req.url.includes('/me')) {
                    return throwError(() => error);
                }

                // Use backend response directly
                const status = error.error?.status ?? error.status;
                let errorName = error.error?.error ?? 'Unexpected error occurred.';
                if (status == 404) errorName = 'Page not found';
                this.errorService.setError(status, errorName);

                if (status === 401) {
                    this.authService.logout();
                    this.router.navigate(['/login']);
                } else {
                    this.router.navigate(['/error']);
                }

                return throwError(() => error);
            })
        );
    }
}