import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { BehaviorSubject, catchError, map, of, switchMap, tap } from 'rxjs';
import { User } from '../models/user.model';

interface AuthResponse {
	status: 'SUCCESS' | 'FAILURE';
	message: string;
	error: any;
}

@Injectable({ providedIn: 'root' })
export class AuthService {

	private baseUrl = environment.apiUrl + 'auth/';

	private userSubject = new BehaviorSubject<User | null>(null);
	user$ = this.userSubject.asObservable();

	loggedIn$ = this.user$.pipe(
		map(user => !!user)
	);

	constructor(private http: HttpClient) { }

	login(username: string, password: string) {
		return this.http.post<AuthResponse>(
			this.baseUrl + 'login',
			{ username, password },
			{ headers: { 'skip-error': 'true' }, withCredentials: true }
		).pipe(
			switchMap(response => {
				if (response.status === 'SUCCESS') {
					return this.me();
				}
				return of(null);
			}),
			tap(user => this.userSubject.next(user))
		);
	}

	logout() {
		return this.http.post(
			this.baseUrl + 'logout',
			{},
			{ withCredentials: true }
		).pipe(
			tap(() => this.userSubject.next(null)),
			catchError(() => {
				this.userSubject.next(null);
				return of(null);
			})
		);
	}

	me() {
		return this.http.get<User>(
			this.baseUrl + 'me',
			{ headers: { 'skip-error': 'true' }, withCredentials: true }
		);
	}

	checkAuth() {
		return this.me().pipe(
			tap(user => this.userSubject.next(user)),
			catchError(() => {
				this.userSubject.next(null);
				return of(null);
			})
		);
	}
}