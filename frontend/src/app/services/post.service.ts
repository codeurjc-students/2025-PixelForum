import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Post } from '../models/post.model';
import { environment } from '../../environments/environment';
import { PageResponse } from '../models/pageResponse.model';

@Injectable({ providedIn: 'root' })
export class PostService {

	private baseUrl = environment.apiUrl + 'posts';

	constructor(private http: HttpClient) { }

	getPosts(
		page: number = 0,
		size: number = 10,
		title?: string,
		authorUsername?: string,
		topic?: string,
		sortBy: string = 'createdAt',
		sortDir: 'asc' | 'desc' = 'desc'
	): Observable<PageResponse<Post>> {

		let params = new HttpParams()
			.set('page', page.toString())
			.set('size', size.toString())
			.set('sort', `${sortBy},${sortDir}`);

		// Add filters if they are provided
		if (title && title.trim()) {
			params = params.set('title', title.trim());
		}

		if (authorUsername && authorUsername.trim()) {
			params = params.set('authorUsername', authorUsername.trim());
		}

		if (topic && topic.trim()) {
			params = params.set('topic', topic.trim());
		}

		return this.http.get<PageResponse<Post>>(this.baseUrl, {
			params,
			withCredentials: true
		});
	}

	getById(id: number): Observable<Post> {
		return this.http.get<Post>(`${this.baseUrl}/${id}`, { withCredentials: true });
	}

	create(post: Post): Observable<Post> {
		return this.http.post<Post>(this.baseUrl, post, { withCredentials: true });
	}

	update(post: Post): Observable<Post> {
		return this.http.put<Post>(`${this.baseUrl}/${post.id}`, post, { withCredentials: true });
	}

	delete(id: number): Observable<void> {
		return this.http.delete<void>(`${this.baseUrl}/${id}`, { withCredentials: true });
	}

}
