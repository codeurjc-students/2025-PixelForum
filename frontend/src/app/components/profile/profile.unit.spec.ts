import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { ProfileComponent } from './profile.component';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { ErrorService } from '../../services/error.service';
import { ActivatedRoute, Router } from '@angular/router';
import { User } from '../../models/user.model';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatDialog } from '@angular/material/dialog';
import { ImageService } from '../../services/image.service';

describe('ProfileComponent - Unit Tests', () => {

    let component: ProfileComponent;
    let fixture: ComponentFixture<ProfileComponent>;
    let userService: jasmine.SpyObj<UserService>;
    let authService: jasmine.SpyObj<AuthService>;
    let errorService: jasmine.SpyObj<ErrorService>;
    let router: jasmine.SpyObj<Router>;
    let imageService: jasmine.SpyObj<ImageService>;
    let dialog: jasmine.SpyObj<MatDialog>;

    const mockUser: User = {
        id: 1,
        username: 'testuser'
    } as any;

    let routeParams$: Subject<any>;
    let authUser$: Subject<any>;

    beforeEach(async () => {
        spyOn(console, 'error');

        userService = jasmine.createSpyObj('UserService', ['getById', 'setAvatar', 'deleteAvatar', 'deleteAccount']);
        errorService = jasmine.createSpyObj('ErrorService', ['setError']);
        router = jasmine.createSpyObj('Router', ['navigate']);
        imageService = jasmine.createSpyObj('ImageService', ['uploadImages']);
        dialog = jasmine.createSpyObj('MatDialog', ['open']);

        // Subjects to control observables in tests
        routeParams$ = new Subject();
        authUser$ = new Subject();

        authService = jasmine.createSpyObj('AuthService', ['checkAuth', 'logout'], {
            user$: authUser$.asObservable()
        });

        const activatedRouteMock = {
            params: routeParams$.asObservable()
        };

        await TestBed.configureTestingModule({
            imports: [ProfileComponent, HttpClientTestingModule],
            providers: [
                { provide: UserService, useValue: userService },
                { provide: AuthService, useValue: authService },
                { provide: ErrorService, useValue: errorService },
                { provide: Router, useValue: router },
                { provide: ActivatedRoute, useValue: activatedRouteMock },
                { provide: ImageService, useValue: imageService },
                { provide: MatDialog, useValue: dialog }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(ProfileComponent);
        component = fixture.componentInstance;
    });

    // ---------- INIT ----------

    it('should create component', () => {
        expect(component).toBeTruthy();
    });

    it('should load user profile on init when userId exists', () => {
        spyOn(component, 'loadUserProfile');

        component.ngOnInit();

        authUser$.next({ id: 1 });
        routeParams$.next({ userId: 1 });

        expect(component.loadUserProfile).toHaveBeenCalledWith(1);
    });

    // ---------- LOAD USER PROFILE ----------

    it('should load user and update state correctly', () => {
        userService.getById.and.returnValue(of(mockUser));

        component.loadUserProfile(1);

        expect(userService.getById).toHaveBeenCalledWith(1);
        expect(component.user).toEqual(mockUser);
        expect(component.filterUsername).toBe('testuser');
        expect(component.isLoadingUser).toBeFalse();
        expect(component.activeTab).toBe('posts');
    });

    it('should set isOwnProfile true when user is the same as current user', () => {
        userService.getById.and.returnValue(of(mockUser));

        component['currentUserId'] = 1;
        component.loadUserProfile(1);

        expect(component.isOwnProfile).toBeTrue();
    });

    it('should set isOwnProfile true when user is ADMIN', () => {
        component.ngOnInit();

        component.user = mockUser;
        authUser$.next({ id: 2, roles: ['ADMIN'] });

        expect(component.isOwnProfile).toBeTrue();
    });

    it('should set isOwnProfile false when user is different', () => {
        userService.getById.and.returnValue(of(mockUser));

        component['currentUserId'] = 2;
        component.loadUserProfile(1);

        expect(component.isOwnProfile).toBeFalse();
    });

    // ---------- AUTH SUBSCRIPTION ----------

    it('should set currentUserId from authService', () => {
        component.ngOnInit();

        authUser$.next({ id: 5 });

        expect(component['currentUserId']).toBe(5);
    });

    it('should set currentUserId to null if no user', () => {
        component.ngOnInit();

        authUser$.next(null);

        expect(component['currentUserId']).toBeNull();
    });

    // ---------- NAVIGATION ----------

    it('should go back if history length > 1', () => {
        spyOn(window.history, 'back');
        spyOnProperty(window.history, 'length').and.returnValue(2);

        component.goBack();

        expect(window.history.back).toHaveBeenCalled();
    });

    it('should navigate to posts if no history', () => {
        spyOnProperty(window.history, 'length').and.returnValue(1);

        component.goBack();

        expect(router.navigate).toHaveBeenCalledWith(['/posts']);
    });

    it('should navigate to edit profile', () => {
        component.user = mockUser;

        component.editProfile();

        expect(router.navigate).toHaveBeenCalledWith(['/users', 1, 'edit']);
    });

    // ---------- DESTROY ----------

    it('should complete destroy$ on ngOnDestroy', () => {
        spyOn(component['destroy$'], 'next');
        spyOn(component['destroy$'], 'complete');

        component.ngOnDestroy();

        expect(component['destroy$'].next).toHaveBeenCalled();
        expect(component['destroy$'].complete).toHaveBeenCalled();
    });

    // ---------- AVATAR ----------

    it('should open upload flow when dialog returns true', () => {
        const afterClosed$ = of(true);

        dialog.open.and.returnValue({
            afterClosed: () => afterClosed$
        } as any);

        const clickSpy = jasmine.createSpy('click');
        spyOn(document, 'querySelector').and.returnValue({
            click: clickSpy
        } as any);

        component.onAvatarEditClick();

        expect(dialog.open).toHaveBeenCalled();
        expect(clickSpy).toHaveBeenCalled();
    });

    it('should trigger delete avatar when dialog returns secondary', () => {
        const afterClosed$ = of('secondary');

        dialog.open.and.returnValue({
            afterClosed: () => afterClosed$
        } as any);

        spyOn(component, 'deleteAvatar');

        component.onAvatarEditClick();

        expect(component.deleteAvatar).toHaveBeenCalled();
    });

    it('should do nothing if no file selected', async () => {
        const event = { target: { files: [] } } as any;

        await component.onImageSelected(event);

        expect(imageService.uploadImages).not.toHaveBeenCalled();
    });

    it('should reject non png/jpg images', async () => {
        const file = { type: 'image/gif' } as File;
        const event = { target: { files: [file] } } as any;

        await component.onImageSelected(event);

        expect(errorService.setError).toHaveBeenCalledWith(400, 'Bad Request', 'Only PNG and JPG images are allowed');
    });

    it('should upload image and update profile', async () => {
        const file = { type: 'image/png' } as File;

        const event = { target: { files: [file] } } as any;

        imageService.uploadImages.and.returnValue(of(['10']));

        spyOn(component as any, 'updateProfileImage');

        await component.onImageSelected(event);

        expect(imageService.uploadImages).toHaveBeenCalled();
        expect((component as any).updateProfileImage).toHaveBeenCalledWith('10');
    });

    it('should update profile image and refresh auth', () => {
        component.user = mockUser;

        userService.setAvatar.and.returnValue(of(mockUser));
        authService.checkAuth.and.returnValue(of());

        component['updateProfileImage']('99');

        expect(userService.setAvatar).toHaveBeenCalledWith(1, 99);
        expect(authService.checkAuth).toHaveBeenCalled();
        expect(component.refreshTrigger).toBe(2);
    });

    it('should delete avatar and reset state', () => {
        component.user = mockUser;

        userService.deleteAvatar.and.returnValue(of(void 0));
        authService.checkAuth.and.returnValue(of());

        component.deleteAvatar();

        expect(userService.deleteAvatar).toHaveBeenCalledWith(1);
        expect(component.user!.avatar).toBeUndefined();
        expect(component.avatarUrl).toBe('');
        expect(authService.checkAuth).toHaveBeenCalled();
        expect(component.refreshTrigger).toBe(2);
    });

    // ---------- DELETE ACCOUNT ----------

    it('should delete account and navigate home when confirmed', () => {
        component.user = mockUser;
        component.loggedUser = { id: 2 } as any;

        dialog.open.and.returnValue({
            afterClosed: () => of(true)
        } as any);

        userService.deleteAccount.and.returnValue(of(void 0));

        component.deleteAccount();

        expect(userService.deleteAccount).toHaveBeenCalledWith(1);
        expect(router.navigate).toHaveBeenCalledWith(['/']);
        expect(authService.logout).not.toHaveBeenCalled();
    });

    it('should logout and navigate home when deleting own account', () => {
        component.user = mockUser;
        component.loggedUser = { id: 1 } as any;

        dialog.open.and.returnValue({
            afterClosed: () => of(true)
        } as any);

        userService.deleteAccount.and.returnValue(of(void 0));
        authService.logout.and.returnValue(of());

        component.deleteAccount();

        expect(userService.deleteAccount).toHaveBeenCalledWith(1);
        expect(authService.logout).toHaveBeenCalled();
        expect(router.navigate).toHaveBeenCalledWith(['/']);
    });

    it('should not delete account when dialog is cancelled', () => {
        component.user = mockUser;

        dialog.open.and.returnValue({
            afterClosed: () => of(false)
        } as any);

        component.deleteAccount();

        expect(userService.deleteAccount).not.toHaveBeenCalled();
        expect(authService.logout).not.toHaveBeenCalled();
        expect(router.navigate).not.toHaveBeenCalled();
    });

});