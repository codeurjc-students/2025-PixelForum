import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Comment } from '../models/comment.model';
import { environment } from '../../environments/environment';
import { PageResponse } from '../models/pageResponse.model';

@Injectable({ providedIn: 'root' })
export class CommentService {

	private baseUrl(postId: number): string {
		return `${environment.apiUrl}posts/${postId}/comments`;
	}

	private userCommentsUrl(userId: number): string {
		return `${environment.apiUrl}users/${userId}`;
	}

	constructor(private http: HttpClient) { }

	getPostComments(
		postId: number,
		page: number = 0,
		size: number = 10,
		sortBy: string = 'createdAt',
		sortDir: 'asc' | 'desc' = 'desc'
	): Observable<PageResponse<Comment>> {
		const params = new HttpParams()
			.set('page', page.toString())
			.set('size', size.toString())
			.set('sort', `${sortBy},${sortDir}`);

		return this.http.get<PageResponse<Comment>>(this.baseUrl(postId), { params, withCredentials: true });
	}

	getById(postId: number, commentId: number): Observable<Comment> {
		return this.http.get<Comment>(`${this.baseUrl(postId)}/${commentId}`, { withCredentials: true });
	}

	create(postId: number, comment: Comment): Observable<Comment> {
		return this.http.post<Comment>(this.baseUrl(postId), comment, { withCredentials: true });
	}

	update(postId: number, commentId: number, comment: Comment): Observable<Comment> {
		return this.http.put<Comment>(`${this.baseUrl(postId)}/${commentId}`, comment, { withCredentials: true });
	}

	delete(postId: number, commentId: number): Observable<void> {
		return this.http.delete<void>(`${this.baseUrl(postId)}/${commentId}`, { withCredentials: true });
	}

	toggleLike(postId: number, commentId: number): Observable<Comment> {
		return this.http.post<Comment>(`${this.baseUrl(postId)}/${commentId}/like`, {}, { withCredentials: true });
	}

	// ── Profile tabs ──────────────────────────────────────────────────────────

	getUserComments(
		userId: number,
		page: number = 0,
		size: number = 10
	): Observable<PageResponse<Comment>> {
		const params = new HttpParams()
			.set('page', page.toString())
			.set('size', size.toString())
			.set('sort', 'createdAt,desc');

		return this.http.get<PageResponse<Comment>>(`${this.userCommentsUrl(userId)}/comments`, { params, withCredentials: true });
	}

	getLikedComments(
		userId: number,
		page: number = 0,
		size: number = 10
	): Observable<PageResponse<Comment>> {
		const params = new HttpParams()
			.set('page', page.toString())
			.set('size', size.toString())
			.set('sort', 'createdAt,desc');

		return this.http.get<PageResponse<Comment>>(`${this.userCommentsUrl(userId)}/liked-comments`, { params, withCredentials: true });
	}
}