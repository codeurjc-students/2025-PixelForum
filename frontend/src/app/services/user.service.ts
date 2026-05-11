import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { User } from '../models/user.model';
import { PageResponse } from '../models/pageResponse.model';
import { Post } from '../models/post.model';

@Injectable({ providedIn: 'root' })
export class UserService {

    private baseUrl = environment.apiUrl + 'users';

    constructor(private http: HttpClient) { }

    getAll(): Observable<User[]> {
        return this.http.get<User[]>(this.baseUrl, { withCredentials: true });
    }

    getById(id: number): Observable<User> {
        return this.http.get<User>(`${this.baseUrl}/${id}`, { withCredentials: true });
    }

    getUserDetails(userId: number): Observable<User> {
        return this.http.get<User>(`${this.baseUrl}/${userId}/details`, { withCredentials: true });
    }

    getLikedPosts(userId: number, page: number, size: number) {
        return this.http.get<PageResponse<Post>>(`${this.baseUrl}/${userId}/liked-posts`, { params: { page, size }, withCredentials: true });
    }

    register(userData: any): Observable<User> {
        return this.http.post<User>(this.baseUrl, userData, { headers: { 'skip-error': 'true' }, withCredentials: true });
    }

    updateProfile(id: number, data: { username: string; email: string; bio: string }): Observable<User> {
        return this.http.patch<User>(`${this.baseUrl}/${id}`, data, { headers: { 'skip-error': 'true' }, withCredentials: true });
    }

    changePassword(id: number, data: { oldPassword: string; newPassword: string }): Observable<void> {
        return this.http.patch<void>(`${this.baseUrl}/${id}/password`, data, { headers: { 'skip-error': 'true' }, withCredentials: true });
    }

    setAvatar(id: number, imageId: number): Observable<User> {
        return this.http.post<User>(`${this.baseUrl}/${id}/avatar?imageId=${imageId}`, {}, { withCredentials: true });
    }

    deleteAvatar(id: number): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/${id}/avatar`, { withCredentials: true });
    }

    deleteAccount(id: number): Observable<void> {
        return this.http.delete<void>(`${this.baseUrl}/${id}`, { withCredentials: true });
    }
}