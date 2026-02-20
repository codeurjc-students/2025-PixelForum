import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { PostListComponent } from './post-list.component';
import { PostService } from '../../services/post.service';
import { Post } from '../../models/post.model';
import { TopicService } from '../../services/topic.service';
import { UserService } from '../../services/user.service';
import { ActivatedRoute, Router } from '@angular/router';

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

	beforeEach(async () => {
		spyOn(console, 'error');

		postService = jasmine.createSpyObj('PostService', ['getAll']);
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
		postService.getAll.and.returnValue(of(mockPosts));

		component.ngOnInit();

		expect(postService.getAll).toHaveBeenCalled();
		expect(component.posts.length).toBe(2);
		expect(component.loading).toBeFalse();
	});

	it('should sort posts by createdAt desc', () => {
		postService.getAll.and.returnValue(of(mockPosts));

		component.loadPosts();

		expect(component.posts[0].title).toBe('New post');
		expect(component.posts[1].title).toBe('Old post');
	});

	it('should set error message on service error', () => {
		postService.getAll.and.returnValue(
			throwError(() => new Error('Backend error'))
		);

		component.loadPosts();

		expect(component.error).toBe('Error loading posts');
		expect(component.loading).toBeFalse();
	});

	it('should show loading message while loading', () => {
		const subject = new Subject<Post[]>();
		postService.getAll.and.returnValue(subject.asObservable());

		fixture.detectChanges();

		const compiled = fixture.nativeElement as HTMLElement;
		expect(compiled.textContent).toContain('Last PostsLoading posts...');
	});

	it('should show error message in template', () => {
		postService.getAll.and.returnValue(
			throwError(() => new Error('Backend error'))
		);

		fixture.detectChanges();

		const compiled = fixture.nativeElement as HTMLElement;
		expect(compiled.textContent).toContain('Last Posts⚠️Error loading posts');
	});

	it('should render posts when data exists', () => {
		postService.getAll.and.returnValue(of(mockPosts));

		component.ngOnInit();
		fixture.detectChanges();

		const compiled = fixture.nativeElement as HTMLElement;
		const postElements = compiled.querySelectorAll('app-post');

		expect(postElements.length).toBe(2);
	});

	it('should show empty state when no posts', () => {
		postService.getAll.and.returnValue(of([]));

		component.ngOnInit();
		fixture.detectChanges();

		const compiled = fixture.nativeElement as HTMLElement;
		expect(compiled.textContent).toContain('No posts yet');
	});
});