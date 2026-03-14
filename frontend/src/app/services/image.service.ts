import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ImageService {
	private baseUrl = environment.apiUrl + 'images/';

	constructor(private http: HttpClient) { }

	uploadImages(files: File[], category: string = 'default'): Observable<{ urls: string[] }> {
		const formData = new FormData();
		files.forEach((file) => {
			formData.append('files', file);
		});
		return this.http.post<{ urls: string[] }>(
			`${this.baseUrl}upload?category=${category}`,
			formData,
			{ withCredentials: true }
		);
	}

	getImageUrl(filename: string, category: string = 'default'): string {
		return `${this.baseUrl}${category}/${filename}`;
	}

	deleteImage(filename: string, category: string = 'default'): Observable<void> {
		return this.http.post<void>(
			`${this.baseUrl}${category}/${filename}/delete`,
			{},
			{ withCredentials: true }
		);
	}
}