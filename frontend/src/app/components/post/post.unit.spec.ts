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
		postServiceSpy = jasmine.createSpyObj('PostService', ['delete']);
		authServiceSpy = jasmine.createSpyObj('AuthService', ['user$'], { user$: of(mockUser) });
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
		expect(component.currentImage).toBe('img1');
	});

	// ---------- IMAGE NAVIGATION ----------

	it('should go to next image', () => {
		component.ngOnInit();

		component.nextImage();
		expect(component.currentImageIndex).toBe(1);
		expect(component.currentImage).toBe('img2');

		component.nextImage();
		expect(component.currentImageIndex).toBe(1);
	});

	it('should go to previous image', () => {
		component.ngOnInit();
		component.currentImageIndex = 1;

		component.prevImage();
		expect(component.currentImageIndex).toBe(0);
		expect(component.currentImage).toBe('img1');

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
		const afterClosedSpy = jasmine.createSpyObj('afterClosed', ['pipe', 'subscribe']);
		afterClosedSpy.pipe.and.returnValue({ subscribe: (fn: any) => fn(true) });
		const dialogRefSpy = { afterClosed: () => afterClosedSpy } as any;
		dialogSpy.open.and.returnValue(dialogRefSpy);

		postServiceSpy.delete.and.returnValue(of(void 0));

		component.openDeleteDialog();

		expect(dialogSpy.open).toHaveBeenCalled();
		expect(postServiceSpy.delete).toHaveBeenCalledWith(1);
		expect(snackBarSpy.open).toHaveBeenCalledWith('Post deleted successfully', 'Close', { duration: 3000 });
	});

	it('should emit postDeleted when deleting in list mode', () => {
		component.mode = 'list';
		spyOn(component.postDeleted, 'emit');
		postServiceSpy.delete.and.returnValue(of(void 0));

		component['deletePost']();

		expect(component.postDeleted.emit).toHaveBeenCalled();
	});

});