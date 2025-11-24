import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { of } from 'rxjs';
import { PostService } from './service/post.service';
import { Post } from './models/post.model';

describe('App', () => {
	const mockPosts: Post[] = [
		{
			id: 1,
			title: 'Test Post',
			content: 'This is a test',
			topics: [],
		}
	];

	// Fake PostService
	const postServiceMock = {
		getAll: () => of(mockPosts)
	};

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [App],
			providers: [
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

	it('should render latest posts title', () => {
		const fixture = TestBed.createComponent(App);
		fixture.detectChanges();
		const compiled = fixture.nativeElement as HTMLElement;

		// check for the title
		expect(compiled.textContent).toContain('Latest posts');
	});
});
