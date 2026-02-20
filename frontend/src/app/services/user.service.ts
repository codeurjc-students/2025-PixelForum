import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {

    private baseUrl = environment.apiUrl + 'users/';

    constructor(private http: HttpClient) {}

    getAll(): Observable<User[]> {
        return this.http.get<User[]>(this.baseUrl, { withCredentials: true });
    }

    getById(id: number): Observable<User> {
        return this.http.get<User>(`${this.baseUrl}${id}`, { withCredentials: true });
    }
}