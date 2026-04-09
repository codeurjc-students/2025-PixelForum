import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PostComponent } from './post.component';
import { Router } from '@angular/router';
import { PostService } from '../../services/post.service';
import { AuthService } from '../../services/auth.service';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { Post } from '../../models/post.model';
import { User } from '../../models/user.model';
import { Topic } from '../../models/topic.model';

describe('PostComponent', () => {

	let component: PostComponent;
	let fixture: ComponentFixture<PostComponent>;

	let postServiceSpy: jasmine.SpyObj<PostService>;
	let authServiceSpy: jasmine.SpyObj<AuthService>;
	let routerSpy: jasmine.SpyObj<Router>;
	let dialogSpy: jasmine.SpyObj<MatDialog>;
	let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

	const mockTopic: Topic = { id: 1, name: 'Angular' };
	const mockUser: User = { id: 1, username: 'testuser', roles: [] };
	const mockPost: Post = {
		id: 1,
		title: 'Test Post',
		content: 'Content',
		images: ['img1', 'img2'],
		author: mockUser,
		topic: mockTopic,
		likes: 0
	};

	beforeEach(async () => {
		postServiceSpy = jasmine.createSpyObj('PostService', ['delete', 'toggleLike']);
		authServiceSpy = jasmine.createSpyObj('AuthService', [], { user$: of(mockUser) });
		authServiceSpy.loggedIn$ = of(true);
		routerSpy = jasmine.createSpyObj('Router', ['navigate']);
		dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
		snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

		await TestBed.configureTestingModule({
			imports: [PostComponent],
			providers: [
				{ provide: PostService, useValue: postServiceSpy },
				{ provide: AuthService, useValue: authServiceSpy },
				{ provide: Router, useValue: routerSpy },
				{ provide: MatDialog, useValue: dialogSpy },
				{ provide: MatSnackBar, useValue: snackBarSpy }
			]
		}).compileComponents();

		fixture = TestBed.createComponent(PostComponent);
		component = fixture.componentInstance;
		component.post = mockPost;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	// ---------- INIT ----------

	it('should initialize values on init', () => {
		component.ngOnInit();

		expect(component.imageCount).toBe(2);
		expect(component.currentImage).toBe('api/v1/images/img1');
		expect(component.topicData).toEqual(mockTopic);
	});

	// ---------- IMAGE NAVIGATION ----------

	it('should go to next image', () => {
		component.ngOnInit();

		component.nextImage();
		expect(component.currentImageIndex).toBe(1);
		expect(component.currentImage).toBe('api/v1/images/img2');

		component.nextImage();
		expect(component.currentImageIndex).toBe(1);
	});

	it('should go to previous image', () => {
		component.ngOnInit();
		component.currentImageIndex = 1;

		component.prevImage();
		expect(component.currentImageIndex).toBe(0);
		expect(component.currentImage).toBe('api/v1/images/img1');

		component.prevImage();
		expect(component.currentImageIndex).toBe(0);
	});

	// ---------- NAVIGATION ----------

	it('should navigate to topic', () => {
		component.goToTopic();
		expect(routerSpy.navigate).toHaveBeenCalledWith(['/posts/topics', 1], { state: { name: 'Angular' } });
	});

	it('should navigate to user', () => {
		component.goToUser();
		expect(routerSpy.navigate).toHaveBeenCalledWith(['/posts/users', 1], { state: { name: 'testuser' } });
	});

	it('should navigate to post', () => {
		component.goToPost();
		expect(routerSpy.navigate).toHaveBeenCalledWith(['/posts', 1]);
	});

	it('should call goToPost on post click in list mode', () => {
		spyOn(component, 'goToPost');
		component.mode = 'list';
		component.onPostClick();
		expect(component.goToPost).toHaveBeenCalled();
	});

	it('should navigate to comments', () => {
		component.goToComments();
		expect(routerSpy.navigate).toHaveBeenCalledWith(['/posts', 1], { fragment: 'comments' });
	});

	// ---------- EDIT ----------

	it('should navigate to edit post', () => {
		component.editPost();
		expect(routerSpy.navigate).toHaveBeenCalledWith(['/posts', 1, 'edit']);
	});

	// ---------- DELETE ----------

	it('should open delete dialog and delete post', () => {
		dialogSpy.open.and.returnValue({ afterClosed: () => of(true) } as any);

		postServiceSpy.delete.and.returnValue(of(void 0));

		component.openDeleteDialog();

		expect(dialogSpy.open).toHaveBeenCalled();
		expect(postServiceSpy.delete).toHaveBeenCalledWith(1);
		expect(snackBarSpy.open).toHaveBeenCalledWith('Post deleted successfully', 'Close', { duration: 3000 });
	});

	it('should navigate when deleting in detail mode', () => {
		component.mode = 'detail';
		postServiceSpy.delete.and.returnValue(of(void 0));

		component['deletePost']();

		expect(routerSpy.navigate).toHaveBeenCalledWith(['/posts']);
	});

	it('should emit postDeleted when deleting in list mode', () => {
		component.mode = 'list';
		spyOn(component.postDeleted, 'emit');
		postServiceSpy.delete.and.returnValue(of(void 0));

		component['deletePost']();

		expect(component.postDeleted.emit).toHaveBeenCalled();
	});

	it('should toggle like when user is logged in', () => {
		const updatedPost: Post = { ...mockPost, likes: 1, hasUserLiked: true };

		postServiceSpy.toggleLike.and.returnValue(of(updatedPost));

		component.toggleLike();

		expect(postServiceSpy.toggleLike).toHaveBeenCalledWith(1);
		expect(component.post).toEqual(updatedPost);
		expect(component.hasUserLiked).toBeTrue();
	});

	it('should remove like when already liked', () => {
		component.hasUserLiked = true;

		const updatedPost: Post = { ...mockPost, likes: 0, hasUserLiked: false };

		postServiceSpy.toggleLike.and.returnValue(of(updatedPost));

		component.toggleLike();

		expect(postServiceSpy.toggleLike).toHaveBeenCalledWith(1);
		expect(component.post.likes).toBe(0);
		expect(component.hasUserLiked).toBeFalse();
	});

	it('should not call toggleLike when user is not logged in', () => {
		authServiceSpy.loggedIn$ = of(false);

		component.toggleLike();

		expect(postServiceSpy.toggleLike).not.toHaveBeenCalled();
	});

});