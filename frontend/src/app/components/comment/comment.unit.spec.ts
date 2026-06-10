import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CommentComponent } from './comment.component';
import { AuthService } from '../../services/auth.service';
import { CommentService } from '../../services/comment.service';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, BehaviorSubject } from 'rxjs';
import { Comment } from '../../models/comment.model';
import { User } from '../../models/user.model';

describe('CommentComponent', () => {

	let component: CommentComponent;
	let fixture: ComponentFixture<CommentComponent>;

	let commentServiceSpy: jasmine.SpyObj<CommentService>;
	let routerSpy: jasmine.SpyObj<Router>;
	let dialogSpy: jasmine.SpyObj<MatDialog>;
	let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

	// BehaviorSubjects so individual tests can push new values without recreating the module
	let userSubject: BehaviorSubject<User | null>;
	let loggedInSubject: BehaviorSubject<boolean>;

	const mockUser: User = { id: 1, username: 'testuser', roles: [] };
	const mockAdmin: User = { id: 99, username: 'admin', roles: ['ADMIN'] };

	const mockComment: Comment = {
		id: 10,
		content: 'This is a test comment',
		post: 5,
		hasUserLiked: false,
		likes: 3,
		author: {
			id: 1,
			username: 'testuser',
			avatar: 'avatar123.jpg'
		} as User
	};

	beforeEach(async () => {
		userSubject = new BehaviorSubject<User | null>(mockUser);
		loggedInSubject = new BehaviorSubject<boolean>(true);

		commentServiceSpy = jasmine.createSpyObj('CommentService', ['toggleLike', 'update', 'delete']);
		routerSpy = jasmine.createSpyObj('Router', ['navigate']);
		dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
		snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

		await TestBed.configureTestingModule({
			imports: [CommentComponent],
			providers: [
				{
					provide: AuthService,
					useValue: { user$: userSubject.asObservable(), loggedIn$: loggedInSubject.asObservable() }
				},
				{ provide: CommentService, useValue: commentServiceSpy },
				{ provide: Router, useValue: routerSpy },
				{ provide: MatDialog, useValue: dialogSpy },
				{ provide: MatSnackBar, useValue: snackBarSpy }
			]
		}).compileComponents();

		fixture = TestBed.createComponent(CommentComponent);
		component = fixture.componentInstance;
		component.comment = { ...mockComment };
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	// ---------- INIT ----------

	it('should initialize hasUserLiked from comment', () => {
		component.comment = { ...mockComment, hasUserLiked: true };
		component.ngOnInit();
		expect(component.hasUserLiked).toBeTrue();
	});

	it('should initialize hasUserLiked as false when comment has not been liked', () => {
		component.comment = { ...mockComment, hasUserLiked: false };
		component.ngOnInit();
		expect(component.hasUserLiked).toBeFalse();
	});

	it('should initialize avatarUrl from comment author avatar', () => {
		component.ngOnInit();
		expect(component.avatarUrl).toBe('api/v1/images/avatar123.jpg?w=240&h=240');
	});

	it('should set isOwner$ to true when logged user is the comment author', (done) => {
		component.ngOnInit();
		component.isOwner$.subscribe(isOwner => {
			expect(isOwner).toBeTrue();
			done();
		});
	});

	it('should set isOwner$ to true when logged user is an admin', (done) => {
		userSubject.next(mockAdmin);
		component.ngOnInit();
		component.isOwner$.subscribe(isOwner => {
			expect(isOwner).toBeTrue();
			done();
		});
	});

	it('should set isOwner$ to false when logged user is not the author nor admin', (done) => {
		const otherUser: User = { id: 55, username: 'other', roles: [] };
		userSubject.next(otherUser);
		component.ngOnInit();
		component.isOwner$.subscribe(isOwner => {
			expect(isOwner).toBeFalse();
			done();
		});
	});

	it('should set isOwner$ to false when no user is logged in', (done) => {
		userSubject.next(null);
		component.ngOnInit();
		component.isOwner$.subscribe(isOwner => {
			expect(isOwner).toBeFalse();
			done();
		});
	});

	// ---------- NAVIGATION ----------

	it('should navigate to user profile when author exists', () => {
		component.goToUser();
		expect(routerSpy.navigate).toHaveBeenCalledWith(
			['/users', 1],
			{ state: { name: 'testuser' } }
		);
	});

	it('should navigate to post using postId input when provided', () => {
		component.postId = 42;
		component.goToPost();
		expect(routerSpy.navigate).toHaveBeenCalledWith(
			['/posts', 42],
			{ state: { scrollToComments: true } }
		);
	});

	it('should navigate to post using comment.post when postId input is not set', () => {
		component.postId = undefined;
		component.comment = { ...mockComment, post: 5 };
		component.goToPost();
		expect(routerSpy.navigate).toHaveBeenCalledWith(
			['/posts', 5],
			{ state: { scrollToComments: true } }
		);
	});

	// ---------- TOGGLE LIKE ----------

	it('should toggle like on comment when user is logged in', () => {
		const updatedComment: Comment = { ...mockComment, likes: 4, hasUserLiked: true };
		commentServiceSpy.toggleLike.and.returnValue(of(updatedComment));

		component.postId = 5;
		component.toggleLike();

		expect(commentServiceSpy.toggleLike).toHaveBeenCalledWith(5, 10);
		expect(component.comment).toEqual(updatedComment);
		expect(component.hasUserLiked).toBeTrue();
	});

	it('should emit commentUnliked when toggling an already liked comment', () => {
		component.hasUserLiked = true;
		const updatedComment: Comment = { ...mockComment, likes: 2, hasUserLiked: false };
		commentServiceSpy.toggleLike.and.returnValue(of(updatedComment));
		spyOn(component.commentUnliked, 'emit');

		component.postId = 5;
		component.toggleLike();

		expect(component.commentUnliked.emit).toHaveBeenCalledWith(mockComment.id);
	});

	it('should not emit commentUnliked when liking a non-liked comment', () => {
		component.hasUserLiked = false;
		const updatedComment: Comment = { ...mockComment, likes: 4, hasUserLiked: true };
		commentServiceSpy.toggleLike.and.returnValue(of(updatedComment));
		spyOn(component.commentUnliked, 'emit');

		component.postId = 5;
		component.toggleLike();

		expect(component.commentUnliked.emit).not.toHaveBeenCalled();
	});

	it('should not call toggleLike when user is not logged in', () => {
		loggedInSubject.next(false);

		component.postId = 5;
		component.toggleLike();

		expect(commentServiceSpy.toggleLike).not.toHaveBeenCalled();
	});

	it('should resolve postId from comment.post when postId input is not set during toggleLike', () => {
		component.postId = undefined;
		component.comment = { ...mockComment, post: 5 };
		const updatedComment: Comment = { ...mockComment, likes: 4, hasUserLiked: true };
		commentServiceSpy.toggleLike.and.returnValue(of(updatedComment));

		component.toggleLike();

		expect(commentServiceSpy.toggleLike).toHaveBeenCalledWith(5, 10);
	});

	// ---------- EDIT ----------

	it('should enter edit mode with current comment content', () => {
		component.startEdit();
		expect(component.isEditing).toBeTrue();
		expect(component.editContent).toBe('This is a test comment');
	});

	it('should cancel edit and clear editContent', () => {
		component.startEdit();
		component.cancelEdit();
		expect(component.isEditing).toBeFalse();
		expect(component.editContent).toBe('');
	});

	it('should save edit and update comment', () => {
		const updatedComment: Comment = { ...mockComment, content: 'Updated content' };
		commentServiceSpy.update.and.returnValue(of(updatedComment));
		spyOn(component.commentUpdated, 'emit');

		component.postId = 5;
		component.editContent = 'Updated content';
		component.isEditing = true;
		component.saveEdit();

		expect(commentServiceSpy.update).toHaveBeenCalledWith(5, 10, { content: 'Updated content' });
		expect(component.comment).toEqual(updatedComment);
		expect(component.isEditing).toBeFalse();
		expect(component.commentUpdated.emit).toHaveBeenCalledWith(updatedComment);
		expect(snackBarSpy.open).toHaveBeenCalledWith('Comment updated', 'Close', { duration: 3000 });
	});

	it('should not save edit if editContent is blank', () => {
		component.postId = 5;
		component.editContent = '   ';
		component.saveEdit();
		expect(commentServiceSpy.update).not.toHaveBeenCalled();
	});

	it('should resolve postId from comment.post when saving edit', () => {
		const updatedComment: Comment = { ...mockComment, content: 'Updated' };
		commentServiceSpy.update.and.returnValue(of(updatedComment));

		component.postId = undefined;
		component.comment = { ...mockComment, post: 5 };
		component.editContent = 'Updated';
		component.saveEdit();

		expect(commentServiceSpy.update).toHaveBeenCalledWith(5, 10, { content: 'Updated' });
	});

	// ---------- DELETE ----------

	it('should open delete dialog and delete comment on confirm', () => {
		dialogSpy.open.and.returnValue({ afterClosed: () => of(true) } as any);
		commentServiceSpy.delete.and.returnValue(of(void 0));
		spyOn(component.commentDeleted, 'emit');

		component.postId = 5;
		component.openDeleteDialog();

		expect(dialogSpy.open).toHaveBeenCalled();
		expect(commentServiceSpy.delete).toHaveBeenCalledWith(5, 10);
		expect(component.commentDeleted.emit).toHaveBeenCalledWith(mockComment.id);
		expect(snackBarSpy.open).toHaveBeenCalledWith('Comment deleted', 'Close', { duration: 3000 });
	});

	it('should not delete comment when dialog is cancelled', () => {
		dialogSpy.open.and.returnValue({ afterClosed: () => of(false) } as any);

		component.postId = 5;
		component.openDeleteDialog();

		expect(commentServiceSpy.delete).not.toHaveBeenCalled();
	});

	it('should resolve postId from comment.post when deleting', () => {
		commentServiceSpy.delete.and.returnValue(of(void 0));
		spyOn(component.commentDeleted, 'emit');

		component.postId = undefined;
		component.comment = { ...mockComment, post: 5 };

		component['deleteComment']();

		expect(commentServiceSpy.delete).toHaveBeenCalledWith(5, 10);
	});

});