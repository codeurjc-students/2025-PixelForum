import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ImageService {
	private baseUrl = environment.apiUrl + 'images';

	constructor(private http: HttpClient) { }

	uploadImages(files: File[]): Observable<string[]> {
		const formData = new FormData();
		files.forEach((file) => {
			formData.append('files', file);
		});
		return this.http.post<string[]>(`${this.baseUrl}`, formData, { withCredentials: true });
	}

	deleteImage(id: number): Observable<void> {
		return this.http.delete<void>(`${this.baseUrl}/${id}`, { withCredentials: true });
	}
}