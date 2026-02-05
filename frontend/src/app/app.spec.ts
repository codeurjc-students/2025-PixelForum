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

	it('should render home page', () => {
		const fixture = TestBed.createComponent(App);
		fixture.detectChanges();
		const compiled = fixture.nativeElement as HTMLElement;

		// Check for presence of child components
		expect(compiled.querySelector('app-header')).not.toBeNull();
		expect(compiled.querySelector('app-sidebar')).not.toBeNull();
		expect(compiled.querySelector('app-post-list')).not.toBeNull();
	});
});
