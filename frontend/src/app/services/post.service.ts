import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Post } from '../models/post.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PostService {

	private baseUrl = environment.apiUrl + 'posts/';

	constructor(private http: HttpClient) { }

	getAll(): Observable<Post[]> {
		return this.http.get<Post[]>(this.baseUrl, { withCredentials: true });
	}

	getById(id: number): Observable<Post> {
		return this.http.get<Post>(`${this.baseUrl}${id}`, { withCredentials: true });
	}

	create(post: Post): Observable<Post> {
		return this.http.post<Post>(this.baseUrl, post, { withCredentials: true });
	}

	update(post: Post): Observable<Post> {
		return this.http.put<Post>(`${this.baseUrl}${post.id}`, post, { withCredentials: true });
	}

	delete(id: number): Observable<void> {
		return this.http.delete<void>(`${this.baseUrl}${id}`, { withCredentials: true });
	}

}
