import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { catchError, Observable, switchMap, throwError } from 'rxjs';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

	private readonly authUrls = ['/auth/login', '/auth/refresh', '/auth/logout'];

	constructor(private http: HttpClient) { }

	intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
		return next.handle(req).pipe(
			catchError((error: HttpErrorResponse) => {
				const isAuthCall = this.authUrls.some(u => req.url.includes(u));
				if (error.status !== 401 || isAuthCall) {
					return throwError(() => error);
				}

				return this.http.post('/api/v1/auth/refresh', {}, { headers: { 'skip-error': 'true' } }).pipe(switchMap(() => next.handle(req)));
			})
		);
	}
}