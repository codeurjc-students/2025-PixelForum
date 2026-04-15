import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { PostListComponent } from './post-list.component';
import { PostService } from '../../services/post.service';
import { Post } from '../../models/post.model';
import { TopicService } from '../../services/topic.service';
import { UserService } from '../../services/user.service';
import { ActivatedRoute, Router } from '@angular/router';
import { PageResponse } from '../../models/pageResponse.model';
import { HttpClientTestingModule } from '@angular/common/http/testing';

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
		pageable: {} as any,
		totalPages: 1,
		totalElements: 2,
		first: true,
		last: true,
		number: 0,
		size: 10,
		numberOfElements: 2,
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
			imports: [PostListComponent, HttpClientTestingModule],
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

	// ---------- INIT ----------

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

	// ---------- SORTING ----------

	it('should sort posts by createdAt desc', () => {
		postService.getPosts.and.returnValue(of(mockPageResponse));

		component.loadPosts();

		expect(component.posts[1].title).toBe('New post');
		expect(component.posts[0].title).toBe('Old post');
	});

	// ---------- LOADING STATE ----------

	it('should show loading message while loading', () => {
		const subject = new Subject<PageResponse<Post>>();
		postService.getPosts.and.returnValue(subject.asObservable());

		fixture.detectChanges();

		const compiled = fixture.nativeElement as HTMLElement;
		expect(compiled.textContent).toContain('Loading posts...');
	});

	// ---------- RENDERING POSTS ----------

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

	// ---------- LOAD MORE & PAGINATION ----------

	it('should load more posts if has more pages', () => {
		component.hasMorePages = true;
		component.currentPage = 0;
		component.isLoadingMore = false;
		postService.getPosts.and.returnValue(of(mockPageResponse));

		component.loadMorePosts();

		expect(component.isLoadingMore).toBeFalse();
		expect(component.currentPage).toBe(1);
		expect(postService.getPosts).toHaveBeenCalled();
	});

	it('should not load more posts if no more pages', () => {
		component.hasMorePages = false;
		component.isLoadingMore = false;

		component.loadMorePosts();

		expect(component.isLoadingMore).toBeFalse();
		expect(component.currentPage).toBe(0);
	});

	// ---------- FETCH POSTS ----------

	it('should append posts on fetchPosts with isInitialLoad false', () => {
		component.posts = [{ id: 0, title: 'Existing', content: '', createdAt: '2024-01-01', topic: { id: 1, name: 'X' } } as any];
		component.currentPage = 0;
		postService.getPosts.and.returnValue(of(mockPageResponse));

		component['fetchPosts'](false);

		expect(component.posts.length).toBe(3);
		expect(component.isLoadingMore).toBeFalse();
	});

	// ---------- REMOVE POST ----------

	it('should reload posts on removePost', () => {
		spyOn(component, 'loadPosts');

		component.removePost();

		expect(component.isLoading).toBeTrue();
		expect(component.loadPosts).toHaveBeenCalled();
	});
});