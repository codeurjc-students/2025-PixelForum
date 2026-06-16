import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { CommentListComponent } from './comment-list.component';
import { CommentService } from '../../services/comment.service';
import { AuthService } from '../../services/auth.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterModule } from '@angular/router';
import { of, BehaviorSubject, Subject } from 'rxjs';
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
				{
					provide: AuthService,
					useValue: {
						loggedIn$: loggedInSubject.asObservable(),
						user$: of(null)
					}
				},
				{ provide: MatSnackBar, useValue: snackBarSpy }
			],
		}).compileComponents();

		fixture = TestBed.createComponent(CommentListComponent);
		component = fixture.componentInstance;
		component.postId = 1;
	});

	it('should create', () => {
		fixture.detectChanges();
		expect(component).toBeTruthy();
	});

	// ---------- INIT ----------

	it('should load comments on init', () => {
		const comments = [mockComment(1), mockComment(2)];
		commentServiceSpy.getPostComments.and.returnValue(of(mockPage(comments)));

		fixture.detectChanges();

		expect(commentServiceSpy.getPostComments).toHaveBeenCalledWith(1, 0, 10);
		expect(component.comments.length).toBe(2);
		expect(component.isLoading).toBeFalse();
	});

	it('should subscribe to loggedIn$ on init', () => {
		loggedInSubject.next(false);
		fixture.detectChanges();
		expect(component.isLoggedIn).toBeFalse();

		loggedInSubject.next(true);
		expect(component.isLoggedIn).toBeTrue();
	});

	it('should complete destroy$ on destroy', () => {
		fixture.detectChanges();
		const nextSpy = spyOn(component['destroy$'], 'next');
		const completeSpy = spyOn(component['destroy$'], 'complete');

		component.ngOnDestroy();

		expect(nextSpy).toHaveBeenCalled();
		expect(completeSpy).toHaveBeenCalled();
	});

	// ---------- CHANGES ----------

	it('should reload comments when refreshTrigger changes', () => {
		fixture.detectChanges();
		spyOn(component, 'loadComments');

		component.refreshTrigger = 1;
		component.ngOnChanges();

		expect(component.loadComments).toHaveBeenCalled();
	});

	it('should reload comments when postId is set and mode is post', () => {
		fixture.detectChanges();
		spyOn(component, 'loadComments');

		component.mode = 'post';
		component.postId = 7;
		component.ngOnChanges();

		expect(component.loadComments).toHaveBeenCalled();
	});

	it('should reload comments when userId is set and mode is user', () => {
		fixture.detectChanges();
		spyOn(component, 'loadComments');

		component.mode = 'user';
		component.userId = 3;
		component.ngOnChanges();

		expect(component.loadComments).toHaveBeenCalled();
	});

	it('should not reload comments when context is missing for the current mode', () => {
		fixture.detectChanges();
		spyOn(component, 'loadComments');

		component.refreshTrigger = undefined!;
		component.mode = 'post';
		component.postId = undefined;
		component.ngOnChanges();

		expect(component.loadComments).not.toHaveBeenCalled();
	});

	// ---------- LOAD COMMENTS ----------

	it('should reset page and comments on loadComments', () => {
		fixture.detectChanges();
		component.currentPage = 3;
		component.comments = [mockComment(1)];

		commentServiceSpy.getPostComments.and.returnValue(of(mockPage([])));
		component.loadComments();

		expect(component.currentPage).toBe(0);
		expect(component.comments).toEqual([]);
	});

	it('should set hasMorePages to true when response is not last', () => {
		const comments = [mockComment(1)];
		commentServiceSpy.getPostComments.and.returnValue(of(mockPage(comments, false, 20)));

		fixture.detectChanges();

		expect(component.hasMorePages).toBeTrue();
	});

	it('should set totalElements from response', () => {
		commentServiceSpy.getPostComments.and.returnValue(of(mockPage([mockComment(1)], true, 42)));

		fixture.detectChanges();

		expect(component.totalElements).toBe(42);
	});

	// ---------- ROUTING BY MODE ----------

	it('should call getUserComments when mode is user', () => {
		commentServiceSpy.getUserComments.and.returnValue(of(mockPage([mockComment(1)])));
		component.mode = 'user';
		component.userId = 3;

		fixture.detectChanges();

		expect(commentServiceSpy.getUserComments).toHaveBeenCalledWith(3, 0, 10);
		expect(commentServiceSpy.getPostComments).not.toHaveBeenCalled();
	});

	it('should call getLikedComments when mode is liked', () => {
		commentServiceSpy.getLikedComments.and.returnValue(of(mockPage([mockComment(1)])));
		component.mode = 'liked';
		component.userId = 3;

		fixture.detectChanges();

		expect(commentServiceSpy.getLikedComments).toHaveBeenCalledWith(3, 0, 10);
		expect(commentServiceSpy.getPostComments).not.toHaveBeenCalled();
	});

	it('should call getPostComments when mode is post', () => {
		commentServiceSpy.getPostComments.and.returnValue(of(mockPage([mockComment(1)])));
		component.mode = 'post';
		component.postId = 1;

		fixture.detectChanges();

		expect(commentServiceSpy.getPostComments).toHaveBeenCalledWith(1, 0, 10);
	});

	// ---------- LOAD MORE ----------

	it('should load more comments when hasMorePages is true', () => {
		commentServiceSpy.getPostComments.and.returnValue(of(mockPage([mockComment(1)], false, 20)));
		fixture.detectChanges();

		const morePage = mockPage([mockComment(2)], true, 20);
		commentServiceSpy.getPostComments.and.returnValue(of(morePage));

		component.loadMoreComments();

		expect(component.currentPage).toBe(1);
		expect(component.comments.length).toBe(2);
		expect(component.isLoadingMore).toBeFalse();
	});

	it('should append comments when loading more', () => {
		commentServiceSpy.getPostComments.and.returnValue(of(mockPage([mockComment(1)], false, 20)));
		fixture.detectChanges();

		commentServiceSpy.getPostComments.and.returnValue(of(mockPage([mockComment(2), mockComment(3)], true, 20)));
		component.loadMoreComments();

		expect(component.comments.map(c => c.id)).toEqual([1, 2, 3]);
	});

	it('should not load more if no more pages', () => {
		fixture.detectChanges();
		component.hasMorePages = false;
		component.isLoadingMore = false;
		const callsBefore = commentServiceSpy.getPostComments.calls.count();

		component.loadMoreComments();

		expect(commentServiceSpy.getPostComments.calls.count()).toBe(callsBefore);
	});

	// ---------- SUBMIT COMMENT ----------

	it('should post a comment and reload pages', () => {
		commentServiceSpy.getPostComments.and.returnValue(of(mockPage([mockComment(1)])));
		fixture.detectChanges();

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
	});

	it('should not submit if content is blank', () => {
		fixture.detectChanges();
		component.newCommentContent = '   ';
		component.submitComment();
		expect(commentServiceSpy.create).not.toHaveBeenCalled();
	});

	it('should not submit if already submitting', () => {
		fixture.detectChanges();
		component.newCommentContent = 'Some text';
		component.isSubmitting = true;
		component.submitComment();
		expect(commentServiceSpy.create).not.toHaveBeenCalled();
	});

	it('should not submit if postId is not set', () => {
		fixture.detectChanges();
		component.postId = undefined;
		component.newCommentContent = 'Some text';
		component.submitComment();
		expect(commentServiceSpy.create).not.toHaveBeenCalled();
	});

	it('should not submit if user is not logged in', () => {
		loggedInSubject.next(false);
		fixture.detectChanges();
		component.newCommentContent = 'Some text';
		component.submitComment();
		expect(commentServiceSpy.create).not.toHaveBeenCalled();
	});

	// ---------- ON COMMENT DELETED ----------

	it('should remove deleted comment from list', () => {
		fixture.detectChanges();
		component.comments = [mockComment(1), mockComment(2), mockComment(3)];
		component.totalElements = 3;
		commentServiceSpy.getPostComments.and.returnValue(of(mockPage([mockComment(1), mockComment(3)])));

		component.onCommentDeleted(2);

		expect(component.comments.find(c => c.id === 2)).toBeUndefined();
	});

	it('should decrement totalElements on comment deleted', () => {
		fixture.detectChanges();
		component.comments = [mockComment(1), mockComment(2)];
		component.totalElements = 2;
		commentServiceSpy.getPostComments.and.returnValue(of(mockPage([mockComment(1)])));

		component.onCommentDeleted(2);

		expect(component.totalElements).toBe(1);
	});

	it('should not go below 0 on totalElements when deleting', () => {
		fixture.detectChanges();
		component.comments = [mockComment(1)];
		component.totalElements = 0;
		commentServiceSpy.getPostComments.and.returnValue(of(mockPage([])));

		component.onCommentDeleted(1);

		expect(component.totalElements).toBe(0);
	});

	it('should emit commentCountChanged with -1 on comment deleted', () => {
		fixture.detectChanges();
		component.comments = [mockComment(1)];
		component.totalElements = 1;
		spyOn(component.commentCountChanged, 'emit');
		commentServiceSpy.getPostComments.and.returnValue(of(mockPage([])));

		component.onCommentDeleted(1);

		expect(component.commentCountChanged.emit).toHaveBeenCalledWith(-1);
	});

	// ---------- ON COMMENT UPDATED ----------

	it('should replace updated comment in the list', () => {
		fixture.detectChanges();
		const original = mockComment(1);
		const updated: Comment = { ...original, content: 'Edited content' };
		component.comments = [original, mockComment(2)];

		component.onCommentUpdated(updated);

		expect(component.comments[0].content).toBe('Edited content');
		expect(component.comments[1].content).toBe('Comment 2');
	});

	// ---------- ON COMMENT UNLIKED ----------

	it('should reload comments when an unliked comment is removed in liked mode', () => {
		component.mode = 'liked';
		component.userId = 3;
		commentServiceSpy.getLikedComments.and.returnValue(of(mockPage([mockComment(1)])));
		fixture.detectChanges();
		spyOn(component, 'loadComments');

		component.onCommentUnliked(1);

		expect(component.loadComments).toHaveBeenCalled();
	});

	it('should not reload comments on unlike when mode is not liked', () => {
		fixture.detectChanges();
		spyOn(component, 'loadComments');

		component.mode = 'post';
		component.onCommentUnliked(1);

		expect(component.loadComments).not.toHaveBeenCalled();
	});

});