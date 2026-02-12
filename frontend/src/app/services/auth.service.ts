import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { BehaviorSubject, catchError, of, tap } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AuthService {

	private baseUrl = environment.apiUrl + 'auth/';

    private loggedInSubject = new BehaviorSubject<boolean>(false);
	loggedIn$ = this.loggedInSubject.asObservable();
    
	constructor(private http: HttpClient) {}
    
	login(username: string, password: string) {
		return this.http.post(
		    this.baseUrl + 'login',
			{ username, password },
			{ withCredentials: true }
		).pipe(
			tap(() => this.loggedInSubject.next(true))
		);
	}

	logout() {
		return this.http.post(
			this.baseUrl + 'logout',
			{},
			{ withCredentials: true }
		).pipe(
			tap(() => { 
				this.loggedInSubject.next(false); 
				window.location.href = '/posts';
			})	
		);
	}

	me() {
		return this.http.get(
			this.baseUrl + 'me',
			{ withCredentials: true }
		);
	}

	checkAuth() {
		return this.me().pipe(
			tap(() => this.loggedInSubject.next(true)),
			catchError(() => {
				this.loggedInSubject.next(false);
				return of(null);
			})
		);
	}
}
