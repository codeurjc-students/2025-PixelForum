import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';

@Injectable({
    providedIn: 'root'
})
export class AuthService {

    private baseUrl = environment.apiUrl + 'auth/';

    constructor(private http: HttpClient) {}

    login(username: string, password: string) {
        return this.http.post<{ token: string }>(
            this.baseUrl + 'login',
            { username, password }
        ).pipe(
            tap(response => {
                localStorage.setItem('jwt', response.token);
            })
        );
    }

    logout(): void {
        localStorage.removeItem('jwt');
    }

    getToken(): string | null {
        return localStorage.getItem('jwt');
    }

    isAuthenticated(): boolean {
        return this.getToken() !== null;
    }
}