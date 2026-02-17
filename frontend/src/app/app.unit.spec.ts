import { TestBed } from '@angular/core/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { provideRouter } from '@angular/router';
import { routes } from './app.routes';

import { App } from './app';
import { PostService } from './services/post.service';
import { Post } from './models/post.model';

describe('App', () => {

	const mockPosts: Post[] = [
		{
			id: 1,
			title: 'Test Post',
			content: 'This is a test',
			topic: {
				id: 1,
				name: 'Test Topic'
			}
		}
	];

	// Mock PostService for unit testing
	const postServiceMock = {
		getAll: () => of(mockPosts)
	};

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [App],
			providers: [
				provideHttpClient(),
				provideHttpClientTesting(),
				provideRouter(routes),
				{ provide: PostService, useValue: postServiceMock }
			],
			schemas: [CUSTOM_ELEMENTS_SCHEMA]
		}).compileComponents();
	});

	it('should create the app', () => {
		const fixture = TestBed.createComponent(App);
		const app = fixture.componentInstance;
		expect(app).toBeTruthy();
	});

	it('should render home page layout components', () => {
		const fixture = TestBed.createComponent(App);
		fixture.detectChanges();
		const compiled = fixture.nativeElement as HTMLElement;

		expect(compiled.querySelector('app-header')).not.toBeNull();
		expect(compiled.querySelector('app-sidebar')).not.toBeNull();
	});
});
