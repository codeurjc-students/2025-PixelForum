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

        if (req.headers.has('skip-error')) {
            return next.handle(req);
        }

        return next.handle(req).pipe(
            catchError((error: HttpErrorResponse) => {
                // Use backend response directly
                const status = error.status;
                if (status === 401) {
                    this.authService.logout();
                    this.router.navigate(['/login']);
                    return throwError(() => error);
                }
                let errorName = error.error?.error ?? 'Unable to connect to the server';
                this.errorService.setError(status, errorName, error.error?.message ?? '');
                this.router.navigate(['/error']);

                return throwError(() => error);
            })
        );
    }
}