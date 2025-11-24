import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Post } from '../models/post.model';

@Injectable({
	providedIn: 'root'
})
export class PostService {

	private baseUrl = 'http://localhost:8080/api/posts/';

	constructor(private http: HttpClient) {}

	getAll(): Observable<Post[]> {
		return this.http.get<Post[]>(this.baseUrl);
	}

	getById(id: number): Observable<Post> {
		return this.http.get<Post>(`${this.baseUrl}/${id}`);
	}

	create(post: Post): Observable<Post> {
		return this.http.post<Post>(this.baseUrl, post);
	}

	update(post: Post): Observable<Post> {
		return this.http.put<Post>(`${this.baseUrl}/${post.id}`, post);
	}

	delete(id: number): Observable<void> {
		return this.http.delete<void>(`${this.baseUrl}/${id}`);
	}
}
