import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject, of, Subject, throwError } from 'rxjs';
import { PostListComponent } from './post-list.component';
import { PostService } from '../../services/post.service';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { Post } from '../../models/post.model';
import { PageResponse } from '../../models/pageResponse.model';

describe('PostListComponent - Unit Tests', () => {

	let component: PostListComponent;
	let fixture: ComponentFixture<PostListComponent>;
	let postService: jasmine.SpyObj<PostService>;
	let userService: jasmine.SpyObj<UserService>;
	let userSubject: BehaviorSubject<any>;

	const mockPost = (id: number): Post => ({
		id,
		title: `Post ${id}`,
		content: `Content ${id}`,
		createdAt: '2024-01-01T10:00:00',
		topic: { id: 1, name: 'GTA VI' } as any
	} as Post);

	const mockPage = (posts: Post[], last = true, total = posts.length): PageResponse<Post> => ({
		content: posts,
		pageable: {} as any,
		totalPages: last ? 1 : 2,
		totalElements: total,
		first: true,
		last,
		number: 0,
		size: 10,
		numberOfElements: posts.length,
		empty: posts.length === 0
	});

	const defaultArgs = (page = 0) => [page, 10, undefined, undefined, undefined, 'createdAt', 'desc'];

	// Set component inputs and trigger change detection
	const setInputs = (inputs: Record<string, unknown>) => {
		Object.entries(inputs).forEach(([key, value]) => fixture.componentRef.setInput(key, value));
		fixture.detectChanges();
	};

	const ids = () => component.posts.map(p => p.id);
	const totalCalls = () => postService.getPosts.calls.count() + userService.getLikedPosts.calls.count();

	beforeEach(async () => {
		spyOn(console, 'error');

		userSubject = new BehaviorSubject<any>(null);

		postService = jasmine.createSpyObj('PostService', ['getPosts']);
		userService = jasmine.createSpyObj('UserService', ['getLikedPosts']);
		postService.getPosts.and.returnValue(of(mockPage([])));
		userService.getLikedPosts.and.returnValue(of(mockPage([])));

		await TestBed.configureTestingModule({
			imports: [PostListComponent],
			providers: [
				{ provide: PostService, useValue: postService },
				{ provide: UserService, useValue: userService },
				{ provide: AuthService, useValue: { user$: userSubject.asObservable(), loggedIn$: of(true) } }
			]
		}).compileComponents();

		fixture = TestBed.createComponent(PostListComponent);
		component = fixture.componentInstance;
	});

	it('should create component', () => {
		expect(component).toBeTruthy();
	});

	// ---------- INIT ----------

	it('should load posts once on init', () => {
		postService.getPosts.and.returnValue(of(mockPage([mockPost(1), mockPost(2)])));

		fixture.detectChanges();

		expect(postService.getPosts).toHaveBeenCalledTimes(1);
		expect(postService.getPosts).toHaveBeenCalledWith(0, 10, undefined, undefined, undefined, 'createdAt', 'desc');
		expect(ids()).toEqual([1, 2]);
		expect(component.isLoading).toBeFalse();
	});

	it('should not load twice when inputs come with the first change', () => {
		setInputs({ filterTopic: 'topic1', refreshTrigger: 0 });
		expect(postService.getPosts).toHaveBeenCalledTimes(1);
	});

	// ---------- USER CHANGES ----------

	it('should reload when the logged user changes', () => {
		fixture.detectChanges();

		userSubject.next({ id: 1 });
		expect(postService.getPosts).toHaveBeenCalledTimes(2);

		userSubject.next(null);
		expect(postService.getPosts).toHaveBeenCalledTimes(3);
	});

	it('should not reload when user$ emits the same user id', () => {
		userSubject.next({ id: 1 });
		fixture.detectChanges();

		userSubject.next({ id: 1, username: 'renamed' });

		expect(postService.getPosts).toHaveBeenCalledTimes(1);
	});

	// ---------- ngOnChanges ----------

	const reloadCases: [string, unknown][] = [
		['filterUsername', 'bob'],
		['filterTopic', 'topic1'],
		['sortBy', 'title'],
		['sortOrder', 'asc'],
		['pageSize', 5],
		['likedByUserId', 5],
		['refreshTrigger', 1]
	];

	reloadCases.forEach(([key, value]) => {
		it(`should reload when ${key} changes`, () => {
			setInputs({ refreshTrigger: 0 });
			const callsBefore = totalCalls();

			setInputs({ [key]: value });

			expect(totalCalls()).toBe(callsBefore + 1);
		});
	});

	// ---------- DESTROY ----------

	it('should complete destroy$ on destroy', () => {
		const nextSpy = spyOn(component['destroy$'], 'next').and.callThrough();
		const completeSpy = spyOn(component['destroy$'], 'complete').and.callThrough();

		component.ngOnDestroy();

		expect(nextSpy).toHaveBeenCalled();
		expect(completeSpy).toHaveBeenCalled();
	});

	it('should ignore in-flight responses after destroy', () => {
		const pending$ = new Subject<PageResponse<Post>>();
		postService.getPosts.and.returnValue(pending$);
		fixture.detectChanges();

		fixture.destroy();
		pending$.next(mockPage([mockPost(1)]));

		expect(component.posts).toEqual([]);
	});

	// ---------- LOAD POSTS ----------

	it('should keep posts in backend order', () => {
		postService.getPosts.and.returnValue(of(mockPage([mockPost(1), mockPost(2)])));

		fixture.detectChanges();

		expect(component.posts[0].title).toBe('Post 1');
		expect(component.posts[1].title).toBe('Post 2');
	});

	it('should call service with filters and sorting', () => {
		setInputs({ filterUsername: 'user1', filterTopic: 'topic1', sortBy: 'title', sortOrder: 'asc', pageSize: 5 });

		expect(postService.getPosts).toHaveBeenCalledWith(0, 5, undefined, 'user1', 'topic1', 'title', 'asc');
	});

	it('should load liked posts when likedByUserId is defined', () => {
		userService.getLikedPosts.and.returnValue(of(mockPage([mockPost(1), mockPost(2)])));

		setInputs({ likedByUserId: 5 });

		expect(userService.getLikedPosts).toHaveBeenCalledWith(5, 0, 10);
		expect(postService.getPosts).not.toHaveBeenCalled();
		expect(ids()).toEqual([1, 2]);
	});

	it('should reset page and posts on loadPosts', () => {
		fixture.detectChanges();
		component.currentPage = 3;
		component.posts = [mockPost(1)];

		component.loadPosts();

		expect(component.currentPage).toBe(0);
		expect(component.posts).toEqual([]);
	});

	it('should set pagination info from the response', () => {
		postService.getPosts.and.returnValue(of(mockPage([mockPost(1)], false, 42)));

		fixture.detectChanges();

		expect(component.hasMorePages).toBeTrue();
		expect(component.totalElements).toBe(42);
		expect(component.totalPages).toBe(2);
	});

	it('should stop loading when the initial request fails', () => {
		postService.getPosts.and.returnValue(throwError(() => new Error('boom')));

		fixture.detectChanges();

		expect(component.isLoading).toBeFalse();
		expect(component.posts).toEqual([]);
	});

	it('should ignore a stale response when a filter changes mid-request', () => {
		const first$ = new Subject<PageResponse<Post>>();
		postService.getPosts.and.returnValues(first$, of(mockPage([mockPost(2)])));

		setInputs({ filterTopic: 'a' });
		setInputs({ filterTopic: 'b' });
		first$.next(mockPage([mockPost(1)]));

		expect(ids()).toEqual([2]);
		expect(component.isLoading).toBeFalse();
	});

	// ---------- LOAD MORE ----------

	it('should append posts when loading more', () => {
		postService.getPosts.and.returnValue(of(mockPage([mockPost(1)], false, 3)));
		fixture.detectChanges();

		postService.getPosts.and.returnValue(of(mockPage([mockPost(2), mockPost(3)], true, 3)));
		component.loadMorePosts();

		expect(component.currentPage).toBe(1);
		expect(postService.getPosts).toHaveBeenCalledWith(1, 10, undefined, undefined, undefined, 'createdAt', 'desc');
		expect(ids()).toEqual([1, 2, 3]);
		expect(component.isLoadingMore).toBeFalse();
		expect(component.hasMorePages).toBeFalse();
	});

	it('should not load more if there are no more pages', () => {
		fixture.detectChanges();
		const callsBefore = totalCalls();

		component.loadMorePosts();

		expect(totalCalls()).toBe(callsBefore);
		expect(component.currentPage).toBe(0);
	});

	it('should not load more if already loading more', () => {
		fixture.detectChanges();
		component.hasMorePages = true;
		component.isLoadingMore = true;
		const callsBefore = totalCalls();

		component.loadMorePosts();

		expect(totalCalls()).toBe(callsBefore);
	});

	it('should not load more during the initial load', () => {
		postService.getPosts.and.returnValue(new Subject<PageResponse<Post>>());
		fixture.detectChanges();
		component.hasMorePages = true;
		const callsBefore = totalCalls();

		component.loadMorePosts();

		expect(component.isLoading).toBeTrue();
		expect(totalCalls()).toBe(callsBefore);
	});

	it('should not load more while reloading', () => {
		postService.getPosts.and.returnValue(of(mockPage([mockPost(1)], false, 20)));
		fixture.detectChanges();

		postService.getPosts.and.returnValue(new Subject<PageResponse<Post>>());
		component.removePost();
		const callsBefore = totalCalls();

		component.loadMorePosts();

		expect(totalCalls()).toBe(callsBefore);
	});

	it('should roll back currentPage when loading more fails', () => {
		postService.getPosts.and.returnValue(of(mockPage([mockPost(1)], false, 20)));
		fixture.detectChanges();

		postService.getPosts.and.returnValue(throwError(() => new Error('boom')));
		component.loadMorePosts();

		expect(component.currentPage).toBe(0);
		expect(component.isLoadingMore).toBeFalse();
		expect(ids()).toEqual([1]);
	});

	// ---------- RELOAD LOADED PAGES ----------

	it('should reload every loaded page on removePost and keep them', () => {
		postService.getPosts.and.callFake((page: number) =>
			of(page === 0
				? mockPage([mockPost(1)], false, 2)
				: mockPage([mockPost(2)], true, 2))
		);
		fixture.detectChanges();
		component.loadMorePosts();
		expect(ids()).toEqual([1, 2]);

		component.removePost();

		const lastTwoCalls = postService.getPosts.calls.allArgs().slice(-2);
		expect(lastTwoCalls).toEqual([defaultArgs(0), defaultArgs(1)] as any);
		expect(ids()).toEqual([1, 2]);
		expect(component.currentPage).toBe(1);
	});

	it('should sync list and totalElements with the server after removePost', () => {
		postService.getPosts.and.returnValue(of(mockPage([mockPost(1), mockPost(2), mockPost(3)])));
		fixture.detectChanges();

		postService.getPosts.and.returnValue(of(mockPage([mockPost(1), mockPost(3)])));
		component.removePost();

		expect(ids()).toEqual([1, 3]);
		expect(component.totalElements).toBe(2);
	});

	it('should not show the spinner nor drop posts while reloading', () => {
		postService.getPosts.and.returnValue(of(mockPage([mockPost(1)])));
		fixture.detectChanges();

		postService.getPosts.and.returnValue(new Subject<PageResponse<Post>>());
		component.removePost();

		expect(component.isLoading).toBeFalse();
		expect(ids()).toEqual([1]);
	});

	it('should clear loading flags if the reload fails', () => {
		fixture.detectChanges();
		postService.getPosts.and.returnValue(throwError(() => new Error('boom')));

		component.removePost();

		expect(component['isReloading']).toBeFalse();
		expect(component.isLoading).toBeFalse();
		expect(component.isLoadingMore).toBeFalse();
	});

	// ---------- REMOVE UNLIKED POST ----------

	it('should reload loaded pages when removing an unliked post from the current user', () => {
		userService.getLikedPosts.and.returnValue(of(mockPage([mockPost(1), mockPost(2)])));
		setInputs({ likedByUserId: 4 });
		const callsBefore = userService.getLikedPosts.calls.count();

		userService.getLikedPosts.and.returnValue(of(mockPage([mockPost(2)])));
		component.removeUnlikedPost(4);

		expect(userService.getLikedPosts.calls.count()).toBe(callsBefore + 1);
		expect(ids()).toEqual([2]);
	});

	it('should not reload if the emitted id belongs to another user', () => {
		setInputs({ likedByUserId: 4 });
		const callsBefore = totalCalls();

		component.removeUnlikedPost(8);

		expect(totalCalls()).toBe(callsBefore);
	});

	it('should not reload on removeUnlikedPost without likedByUserId or id', () => {
		fixture.detectChanges();
		const callsBefore = totalCalls();

		component.removeUnlikedPost(4);
		component.removeUnlikedPost(undefined);

		expect(totalCalls()).toBe(callsBefore);
	});

	// ---------- RENDERING ----------

	it('should show loading message while loading', () => {
		postService.getPosts.and.returnValue(new Subject<PageResponse<Post>>());

		fixture.detectChanges();

		expect((fixture.nativeElement as HTMLElement).textContent).toContain('Loading posts...');
	});

	it('should render posts when data exists', () => {
		postService.getPosts.and.returnValue(of(mockPage([mockPost(1), mockPost(2)])));

		fixture.detectChanges();

		expect((fixture.nativeElement as HTMLElement).querySelectorAll('app-post').length).toBe(2);
	});

	it('should show empty state when there are no posts', () => {
		fixture.detectChanges();

		expect((fixture.nativeElement as HTMLElement).textContent).toContain('No posts yet');
	});

});