import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Topic } from '../models/topic.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class TopicService {

    private baseUrl = environment.apiUrl + 'topics/';

    constructor(private http: HttpClient) {}

    getAll(): Observable<Topic[]> {
        return this.http.get<Topic[]>(this.baseUrl, { withCredentials: true });
    }

    getById(id: number): Observable<Topic> {
        return this.http.get<Topic>(`${this.baseUrl}${id}`, { withCredentials: true });
    }
}