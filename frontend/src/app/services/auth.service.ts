import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { BehaviorSubject, catchError, of, tap } from 'rxjs';
import { User } from '../models/user.model';

interface AuthResponse {
	status: 'SUCCESS' | 'FAILURE';
	message: string;
	error: any;
}

@Injectable({ providedIn: 'root' })
export class AuthService {

	private baseUrl = environment.apiUrl + 'auth/';

    private loggedInSubject = new BehaviorSubject<boolean>(false);
	loggedIn$ = this.loggedInSubject.asObservable();
    
	constructor(private http: HttpClient) {}
    
	login(username: string, password: string) {
		return this.http.post<AuthResponse>(
		    this.baseUrl + 'login',
			{ username, password },
			{ withCredentials: true }
		).pipe(
			tap(response => {
				if (response.status === 'SUCCESS') {
					this.loggedInSubject.next(true);
				}
			})
		);
	}

	logout() {
		return this.http.post(
			this.baseUrl + 'logout',
			{},
			{ withCredentials: true }
		).pipe(
			tap(() => this.loggedInSubject.next(false))
		);
	}

	me() {
		return this.http.get<User>(
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