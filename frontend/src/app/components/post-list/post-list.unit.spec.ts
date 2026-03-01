import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { PostListComponent } from './post-list.component';
import { PostService } from '../../services/post.service';
import { Post } from '../../models/post.model';
import { TopicService } from '../../services/topic.service';
import { UserService } from '../../services/user.service';
import { ActivatedRoute, Router } from '@angular/router';
import { PageResponse } from '../../models/pageResponse.model';

describe('PostListComponent - Unit Tests', () => {
	let component: PostListComponent;
	let fixture: ComponentFixture<PostListComponent>;
	let postService: jasmine.SpyObj<PostService>;
	let topicService: jasmine.SpyObj<TopicService>;
	let userService: jasmine.SpyObj<UserService>;
	let router: jasmine.SpyObj<Router>;

	const mockPosts: Post[] = [
		{
			id: 1,
			title: 'Old post',
			content: 'Old content',
			createdAt: '2024-01-01T10:00:00Z',
			topic: { id: 1, name: 'GTA VI' } as any,
		},
		{
			id: 2,
			title: 'New post',
			content: 'New content',
			createdAt: '2024-02-01T10:00:00Z',
			topic: { id: 1, name: 'GTA VI' } as any
		}
	];

	const mockPageResponse: PageResponse<Post> = {
		content: mockPosts,
		pageable: {
			pageNumber: 0,
			pageSize: 10,
			sort: { empty: false, sorted: true, unsorted: false },
			offset: 0,
			paged: true,
			unpaged: false
		},
		totalPages: 1,
		totalElements: mockPosts.length,
		first: true,
		last: true,
		number: 0,
		size: 10,
		numberOfElements: mockPosts.length,
		empty: false
	};

	beforeEach(async () => {
		spyOn(console, 'error');

		postService = jasmine.createSpyObj('PostService', ['getPosts']);
		topicService = jasmine.createSpyObj('TopicService', ['getById']);
		userService = jasmine.createSpyObj('UserService', ['getById']);
		router = jasmine.createSpyObj('Router', ['navigate']);

		const activatedRouteMock = {
			params: of({})
		};

		await TestBed.configureTestingModule({
			imports: [PostListComponent],
			providers: [
				{ provide: PostService, useValue: postService },
				{ provide: TopicService, useValue: topicService },
				{ provide: UserService, useValue: userService },
				{ provide: Router, useValue: router },
				{ provide: ActivatedRoute, useValue: activatedRouteMock }
			]
		}).compileComponents();

		fixture = TestBed.createComponent(PostListComponent);
		component = fixture.componentInstance;
	});

	it('should create component', () => {
		expect(component).toBeTruthy();
	});

	it('should load posts on init', () => {
		postService.getPosts.and.returnValue(of(mockPageResponse));

		component.ngOnInit();

		expect(postService.getPosts).toHaveBeenCalled();
		expect(component.posts.length).toBe(2);
		expect(component.isLoading).toBeFalse();
	});

	it('should sort posts by createdAt desc', () => {
		postService.getPosts.and.returnValue(of(mockPageResponse));

		component.loadPosts();

		expect(component.posts[1].title).toBe('New post');
		expect(component.posts[0].title).toBe('Old post');
	});

	it('should show loading message while loading', () => {
		const subject = new Subject<PageResponse<Post>>();
		postService.getPosts.and.returnValue(subject.asObservable());

		fixture.detectChanges();

		const compiled = fixture.nativeElement as HTMLElement;
		expect(compiled.textContent).toContain('Latest PostsLoading posts...');
	});

	it('should render posts when data exists', () => {
		postService.getPosts.and.returnValue(of(mockPageResponse));

		component.ngOnInit();
		fixture.detectChanges();

		const compiled = fixture.nativeElement as HTMLElement;
		const postElements = compiled.querySelectorAll('app-post');

		expect(postElements.length).toBe(2);
	});

	it('should show empty state when no posts', () => {
		postService.getPosts.and.returnValue(of({ content: [], totalPages: 0, totalElements: 0, last: true } as any));

		component.ngOnInit();
		fixture.detectChanges();

		const compiled = fixture.nativeElement as HTMLElement;
		expect(compiled.textContent).toContain('No posts yet');
	});
});