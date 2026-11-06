import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CommentListComponent } from './comment-list.component';
import { CommentService } from '../../services/comment.service';
import { AuthService } from '../../services/auth.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterModule } from '@angular/router';
import { of, throwError, BehaviorSubject, Subject } from 'rxjs';
import { Comment } from '../../models/comment.model';
import { PageResponse } from '../../models/pageResponse.model';

describe('CommentListComponent', () => {

	let component: CommentListComponent;
	let fixture: ComponentFixture<CommentListComponent>;

	let commentServiceSpy: jasmine.SpyObj<CommentService>;
	let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
	let loggedInSubject: BehaviorSubject<boolean>;

	const mockComment = (id: number): Comment => ({
		id,
		content: `Comment ${id}`,
		post: 1,
		hasUserLiked: false,
		likes: 0,
		author: { id: 1, username: 'testuser', roles: [] } as any
	});

	const mockPage = (comments: Comment[], last = true, total = comments.length): PageResponse<Comment> => ({
		content: comments,
		pageable: {} as any,
		totalPages: last ? 1 : 2,
		totalElements: total,
		first: true,
		last,
		number: 0,
		size: 10,
		numberOfElements: comments.length,
		empty: comments.length === 0
	});

	// Set component inputs and trigger change detection
	const setInputs = (inputs: Record<string, unknown>) => {
		Object.entries(inputs).forEach(([key, value]) => fixture.componentRef.setInput(key, value));
		fixture.detectChanges();
	};

	const ids = () => component.comments.map(c => c.id);

	beforeEach(async () => {
		loggedInSubject = new BehaviorSubject<boolean>(true);

		commentServiceSpy = jasmine.createSpyObj('CommentService', [
			'getPostComments', 'getUserComments', 'getLikedComments', 'create'
		]);
		snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

		commentServiceSpy.getPostComments.and.returnValue(of(mockPage([])));
		commentServiceSpy.getUserComments.and.returnValue(of(mockPage([])));
		commentServiceSpy.getLikedComments.and.returnValue(of(mockPage([])));

		await TestBed.configureTestingModule({
			imports: [CommentListComponent, RouterModule.forRoot([])],
			providers: [
				{ provide: CommentService, useValue: commentServiceSpy },
				{ provide: AuthService, useValue: { loggedIn$: loggedInSubject.asObservable(), user$: of(null) } },
				{ provide: MatSnackBar, useValue: snackBarSpy }
			],
		}).compileComponents();

		fixture = TestBed.createComponent(CommentListComponent);
		component = fixture.componentInstance;
	});

	it('should create', () => {
		fixture.detectChanges();
		expect(component).toBeTruthy();
	});

	// ---------- AUTH / DESTROY ----------

	it('should track loggedIn$', () => {
		expect(component.isLoggedIn).toBeTrue();

		loggedInSubject.next(false);
		expect(component.isLoggedIn).toBeFalse();

		loggedInSubject.next(true);
		expect(component.isLoggedIn).toBeTrue();
	});

	it('should complete destroy$ on destroy', () => {
		const nextSpy = spyOn(component['destroy$'], 'next').and.callThrough();
		const completeSpy = spyOn(component['destroy$'], 'complete').and.callThrough();

		component.ngOnDestroy();

		expect(nextSpy).toHaveBeenCalled();
		expect(completeSpy).toHaveBeenCalled();
	});

	it('should ignore in-flight responses after destroy', () => {
		const pending$ = new Subject<PageResponse<Comment>>();
		commentServiceSpy.getPostComments.and.returnValue(pending$);
		setInputs({ postId: 1 });

		fixture.destroy();
		pending$.next(mockPage([mockComment(1)]));

		expect(component.comments).toEqual([]);
	});

	// ---------- ngOnChanges ----------

	it('should load comments once on first change', () => {
		const comments = [mockComment(1), mockComment(2)];
		commentServiceSpy.getPostComments.and.returnValue(of(mockPage(comments)));

		setInputs({ postId: 1 });

		expect(commentServiceSpy.getPostComments).toHaveBeenCalledTimes(1);
		expect(commentServiceSpy.getPostComments).toHaveBeenCalledWith(1, 0, 10);
		expect(ids()).toEqual([1, 2]);
		expect(component.isLoading).toBeFalse();
	});

	it('should load once even if refreshTrigger comes with the context', () => {
		setInputs({ postId: 1, refreshTrigger: 0 });
		expect(commentServiceSpy.getPostComments).toHaveBeenCalledTimes(1);
	});

	it('should reload when refreshTrigger changes', () => {
		setInputs({ postId: 1 });
		setInputs({ refreshTrigger: 1 });
		expect(commentServiceSpy.getPostComments).toHaveBeenCalledTimes(2);
	});

	it('should reload when postId changes', () => {
		setInputs({ postId: 1 });
		setInputs({ postId: 7 });

		expect(commentServiceSpy.getPostComments).toHaveBeenCalledTimes(2);
		expect(commentServiceSpy.getPostComments).toHaveBeenCalledWith(7, 0, 10);
	});

	it('should reload when mode and userId change', () => {
		setInputs({ postId: 1 });
		setInputs({ mode: 'user', userId: 3 });

		expect(commentServiceSpy.getUserComments).toHaveBeenCalledWith(3, 0, 10);
	});

	it('should not reload when an unrelated input changes', () => {
		setInputs({ postId: 1 });
		setInputs({ emptyMessage: 'Nothing here' });

		expect(commentServiceSpy.getPostComments).toHaveBeenCalledTimes(1);
	});

	it('should not load when context is missing for the current mode', () => {
		setInputs({ mode: 'post', refreshTrigger: 1 });
		expect(commentServiceSpy.getPostComments).not.toHaveBeenCalled();
	});

	it('should not load in user mode without userId', () => {
		setInputs({ mode: 'user', postId: 1 });

		expect(commentServiceSpy.getUserComments).not.toHaveBeenCalled();
		expect(commentServiceSpy.getPostComments).not.toHaveBeenCalled();
	});

	// ---------- LOAD COMMENTS ----------

	it('should reset page and comments on loadComments', () => {
		setInputs({ postId: 1 });
		component.currentPage = 3;
		component.comments = [mockComment(1)];

		component.loadComments();

		expect(component.currentPage).toBe(0);
		expect(component.comments).toEqual([]);
	});

	it('should set hasMorePages to true when response is not last', () => {
		commentServiceSpy.getPostComments.and.returnValue(of(mockPage([mockComment(1)], false, 20)));
		setInputs({ postId: 1 });
		expect(component.hasMorePages).toBeTrue();
	});

	it('should set totalElements and totalPages from response', () => {
		commentServiceSpy.getPostComments.and.returnValue(of(mockPage([mockComment(1)], false, 42)));
		setInputs({ postId: 1 });

		expect(component.totalElements).toBe(42);
		expect(component.totalPages).toBe(2);
	});

	it('should stop loading when the initial request fails', () => {
		commentServiceSpy.getPostComments.and.returnValue(throwError(() => new Error('boom')));
		setInputs({ postId: 1 });

		expect(component.isLoading).toBeFalse();
		expect(component.comments).toEqual([]);
	});

	it('should ignore a stale response when the context changes mid-request', () => {
		const first$ = new Subject<PageResponse<Comment>>();
		commentServiceSpy.getPostComments.and.returnValues(first$, of(mockPage([mockComment(2)])));

		setInputs({ postId: 1 });
		setInputs({ postId: 2 });
		first$.next(mockPage([mockComment(1)]));

		expect(ids()).toEqual([2]);
		expect(component.isLoading).toBeFalse();
	});

	// ---------- ROUTING BY MODE ----------

	it('should call getUserComments when mode is user', () => {
		commentServiceSpy.getUserComments.and.returnValue(of(mockPage([mockComment(1)])));
		setInputs({ mode: 'user', userId: 3 });

		expect(commentServiceSpy.getUserComments).toHaveBeenCalledWith(3, 0, 10);
		expect(commentServiceSpy.getPostComments).not.toHaveBeenCalled();
		expect(commentServiceSpy.getLikedComments).not.toHaveBeenCalled();
	});

	it('should call getLikedComments when mode is liked', () => {
		commentServiceSpy.getLikedComments.and.returnValue(of(mockPage([mockComment(1)])));
		setInputs({ mode: 'liked', userId: 3 });

		expect(commentServiceSpy.getLikedComments).toHaveBeenCalledWith(3, 0, 10);
		expect(commentServiceSpy.getPostComments).not.toHaveBeenCalled();
		expect(commentServiceSpy.getUserComments).not.toHaveBeenCalled();
	});

	it('should call getPostComments when mode is post', () => {
		setInputs({ mode: 'post', postId: 1 });

		expect(commentServiceSpy.getPostComments).toHaveBeenCalledWith(1, 0, 10);
		expect(commentServiceSpy.getUserComments).not.toHaveBeenCalled();
		expect(commentServiceSpy.getLikedComments).not.toHaveBeenCalled();
	});

	// ---------- LOAD MORE ----------

	it('should append comments when loading more', () => {
		commentServiceSpy.getPostComments.and.returnValue(of(mockPage([mockComment(1)], false, 3)));
		setInputs({ postId: 1 });

		commentServiceSpy.getPostComments.and.returnValue(of(mockPage([mockComment(2), mockComment(3)], true, 3)));
		component.loadMoreComments();

		expect(component.currentPage).toBe(1);
		expect(commentServiceSpy.getPostComments).toHaveBeenCalledWith(1, 1, 10);
		expect(ids()).toEqual([1, 2, 3]);
		expect(component.isLoadingMore).toBeFalse();
		expect(component.hasMorePages).toBeFalse();
	});

	it('should not load more if there are no more pages', () => {
		setInputs({ postId: 1 });
		const callsBefore = commentServiceSpy.getPostComments.calls.count();

		component.loadMoreComments();

		expect(commentServiceSpy.getPostComments.calls.count()).toBe(callsBefore);
	});

	it('should not load more while already loading more', () => {
		setInputs({ postId: 1 });
		component.hasMorePages = true;
		component.isLoadingMore = true;
		const callsBefore = commentServiceSpy.getPostComments.calls.count();

		component.loadMoreComments();

		expect(commentServiceSpy.getPostComments.calls.count()).toBe(callsBefore);
	});

	it('should not load more during the initial load', () => {
		commentServiceSpy.getPostComments.and.returnValue(new Subject<PageResponse<Comment>>());
		setInputs({ postId: 1 });
		component.hasMorePages = true;
		const callsBefore = commentServiceSpy.getPostComments.calls.count();

		component.loadMoreComments();

		expect(component.isLoading).toBeTrue();
		expect(commentServiceSpy.getPostComments.calls.count()).toBe(callsBefore);
	});

	it('should not load more while reloading', () => {
		commentServiceSpy.getPostComments.and.returnValue(of(mockPage([mockComment(1), mockComment(2)], false, 20)));
		setInputs({ postId: 1 });

		commentServiceSpy.getPostComments.and.returnValue(new Subject<PageResponse<Comment>>());
		component.onCommentDeleted(2);
		const callsBefore = commentServiceSpy.getPostComments.calls.count();

		component.loadMoreComments();

		expect(commentServiceSpy.getPostComments.calls.count()).toBe(callsBefore);
	});

	it('should roll back currentPage when loading more fails', () => {
		commentServiceSpy.getPostComments.and.returnValue(of(mockPage([mockComment(1)], false, 20)));
		setInputs({ postId: 1 });

		commentServiceSpy.getPostComments.and.returnValue(throwError(() => new Error('boom')));
		component.loadMoreComments();

		expect(component.currentPage).toBe(0);
		expect(component.isLoadingMore).toBeFalse();
		expect(ids()).toEqual([1]);
	});

	// ---------- SUBMIT COMMENT ----------

	it('should post a comment, emit +1 and reload', () => {
		commentServiceSpy.getPostComments.and.returnValue(of(mockPage([mockComment(1)])));
		setInputs({ postId: 1 });

		const created = mockComment(99);
		commentServiceSpy.create.and.returnValue(of(created));
		commentServiceSpy.getPostComments.and.returnValue(of(mockPage([mockComment(1), created])));
		spyOn(component.commentCountChanged, 'emit');

		component.newCommentContent = 'New comment text';
		component.submitComment();

		expect(commentServiceSpy.create).toHaveBeenCalledWith(1, { content: 'New comment text' });
		expect(component.newCommentContent).toBe('');
		expect(component.isSubmitting).toBeFalse();
		expect(component.commentCountChanged.emit).toHaveBeenCalledWith(+1);
		expect(snackBarSpy.open).toHaveBeenCalledWith('Comment posted', 'Close', { duration: 3000 });
		expect(ids()).toEqual([1, 99]);
	});

	it('should keep the text and reset isSubmitting when create fails', () => {
		setInputs({ postId: 1 });
		commentServiceSpy.create.and.returnValue(throwError(() => new Error('boom')));
		spyOn(component.commentCountChanged, 'emit');

		component.newCommentContent = 'New comment text';
		component.submitComment();

		expect(component.isSubmitting).toBeFalse();
		expect(component.newCommentContent).toBe('New comment text');
		expect(component.commentCountChanged.emit).not.toHaveBeenCalled();
		expect(snackBarSpy.open).not.toHaveBeenCalled();
	});

	it('should not notify if the component is destroyed before create responds', () => {
		setInputs({ postId: 1 });
		const create$ = new Subject<Comment>();
		commentServiceSpy.create.and.returnValue(create$);

		component.newCommentContent = 'New comment text';
		component.submitComment();
		fixture.destroy();
		create$.next(mockComment(99));

		expect(snackBarSpy.open).not.toHaveBeenCalled();
	});

	it('should not submit if already submitting', () => {
		setInputs({ postId: 1 });
		component.newCommentContent = 'Some text';
		component.isSubmitting = true;

		component.submitComment();

		expect(commentServiceSpy.create).not.toHaveBeenCalled();
	});

	it('should not submit if postId is not set', () => {
		fixture.detectChanges();
		component.newCommentContent = 'Some text';

		component.submitComment();

		expect(commentServiceSpy.create).not.toHaveBeenCalled();
	});

	it('should not submit if user is not logged in', () => {
		setInputs({ postId: 1 });
		loggedInSubject.next(false);
		component.newCommentContent = 'Some text';

		component.submitComment();

		expect(commentServiceSpy.create).not.toHaveBeenCalled();
	});

	it('should not submit empty content', () => {
		setInputs({ postId: 1 });
		component.newCommentContent = '';

		component.submitComment();

		expect(commentServiceSpy.create).not.toHaveBeenCalled();
	});

	it('should not submit when content contains only whitespace', () => {
		setInputs({ postId: 5 });
		component.newCommentContent = '   \n   ';

		component.submitComment();

		expect(commentServiceSpy.create).not.toHaveBeenCalled();
	});

	it('should not submit when content exceeds max length', () => {
		setInputs({ postId: 5 });
		component.newCommentContent = 'a'.repeat(component.commentRules.maxLength + 1);

		component.submitComment();

		expect(commentServiceSpy.create).not.toHaveBeenCalled();
	});

	it('should normalize content before submitting', () => {
		commentServiceSpy.create.and.returnValue(of(mockComment(99)));
		setInputs({ postId: 5 });

		component.newCommentContent = '  First line  \n  Second line  ';
		component.submitComment();

		expect(commentServiceSpy.create).toHaveBeenCalledWith(5, { content: 'First line\nSecond line' });
	});

	// ---------- RELOAD LOADED PAGES  -------------

	it('should reload every loaded page and keep them after a change', () => {
		commentServiceSpy.getPostComments.and.callFake((_id: number, page: number) =>
			of(page === 0
				? mockPage([mockComment(1)], false, 2)
				: mockPage([mockComment(2)], true, 2))
		);
		setInputs({ postId: 1 });
		component.loadMoreComments();
		expect(ids()).toEqual([1, 2]);

		commentServiceSpy.create.and.returnValue(of(mockComment(99)));
		component.newCommentContent = 'Hi';
		component.submitComment();

		const lastTwoCalls = commentServiceSpy.getPostComments.calls.allArgs().slice(-2);
		expect(lastTwoCalls).toEqual([[1, 0, 10], [1, 1, 10]]);
		expect(ids()).toEqual([1, 2]);
		expect(component.currentPage).toBe(1);
	});

	it('should clear loading flags if the reload fails', () => {
		setInputs({ postId: 1 });
		commentServiceSpy.getPostComments.and.returnValue(throwError(() => new Error('boom')));

		component.onCommentDeleted(1);

		expect(component['isReloading']).toBeFalse();
		expect(component.isLoading).toBeFalse();
		expect(component.isLoadingMore).toBeFalse();
	});

	// ---------- ON COMMENT DELETED ----------

	it('should remove the deleted comment immediately, before the reload ends', () => {
		commentServiceSpy.getPostComments.and.returnValue(
			of(mockPage([mockComment(1), mockComment(2), mockComment(3)]))
		);
		setInputs({ postId: 1 });

		commentServiceSpy.getPostComments.and.returnValue(new Subject<PageResponse<Comment>>());
		component.onCommentDeleted(2);

		expect(ids()).toEqual([1, 3]);
	});

	it('should sync list and totalElements with the server after deleting', () => {
		commentServiceSpy.getPostComments.and.returnValue(
			of(mockPage([mockComment(1), mockComment(2), mockComment(3)]))
		);
		setInputs({ postId: 1 });

		commentServiceSpy.getPostComments.and.returnValue(of(mockPage([mockComment(1), mockComment(3)])));
		component.onCommentDeleted(2);

		expect(ids()).toEqual([1, 3]);
		expect(component.totalElements).toBe(2);
	});

	it('should emit commentCountChanged with -1 on comment deleted', () => {
		commentServiceSpy.getPostComments.and.returnValue(of(mockPage([mockComment(1)])));
		setInputs({ postId: 1 });
		spyOn(component.commentCountChanged, 'emit');

		commentServiceSpy.getPostComments.and.returnValue(of(mockPage([])));
		component.onCommentDeleted(1);

		expect(component.commentCountChanged.emit).toHaveBeenCalledWith(-1);
	});

	// ---------- ON COMMENT UPDATED ----------

	it('should replace the updated comment in the list', () => {
		setInputs({ postId: 1 });
		const original = mockComment(1);
		component.comments = [original, mockComment(2)];

		component.onCommentUpdated({ ...original, content: 'Edited content' });

		expect(component.comments[0].content).toBe('Edited content');
		expect(component.comments[1].content).toBe('Comment 2');
	});

	it('should ignore an updated comment that is not in the list', () => {
		setInputs({ postId: 1 });
		component.comments = [mockComment(1)];

		component.onCommentUpdated(mockComment(42));

		expect(ids()).toEqual([1]);
	});

	// ---------- ON COMMENT UNLIKED ----------

	it('should reload loaded pages when a comment is unliked in liked mode', () => {
		commentServiceSpy.getLikedComments.and.returnValue(of(mockPage([mockComment(1), mockComment(2)])));
		setInputs({ mode: 'liked', userId: 3 });
		const callsBefore = commentServiceSpy.getLikedComments.calls.count();

		commentServiceSpy.getLikedComments.and.returnValue(of(mockPage([mockComment(2)])));
		component.onCommentUnliked(1);

		expect(commentServiceSpy.getLikedComments.calls.count()).toBe(callsBefore + 1);
		expect(ids()).toEqual([2]);
	});

	it('should keep loaded pages and not show the spinner when unliking in liked mode', () => {
		commentServiceSpy.getLikedComments.and.returnValue(of(mockPage([mockComment(1)], false, 2)));
		setInputs({ mode: 'liked', userId: 3 });

		commentServiceSpy.getLikedComments.and.returnValue(new Subject<PageResponse<Comment>>());
		component.onCommentUnliked(1);

		expect(component.isLoading).toBeFalse();
		expect(ids()).toEqual([1]);
	});

	it('should not reload on unlike when mode is not liked', () => {
		setInputs({ postId: 1 });
		const callsBefore = commentServiceSpy.getPostComments.calls.count();

		component.onCommentUnliked(1);

		expect(commentServiceSpy.getPostComments.calls.count()).toBe(callsBefore);
	});

});