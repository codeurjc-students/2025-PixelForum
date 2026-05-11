import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { EditProfileComponent } from './edit-profile.component';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { ActivatedRoute, Router } from '@angular/router';
import { User } from '../../models/user.model';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

describe('EditProfileComponent - Unit Tests', () => {

    let component: EditProfileComponent;
    let fixture: ComponentFixture<EditProfileComponent>;
    let userService: jasmine.SpyObj<UserService>;
    let authService: jasmine.SpyObj<AuthService>;
    let router: jasmine.SpyObj<Router>;
    let snackBar: jasmine.SpyObj<MatSnackBar>;
    let dialog: jasmine.SpyObj<MatDialog>;

    const mockUser: User = {
        id: 1,
        username: 'testuser',
        email: 'test@test.com',
        bio: 'bio'
    } as any;

    let routeParams$: Subject<any>;

    beforeEach(async () => {
        userService = jasmine.createSpyObj('UserService', ['getUserDetails', 'updateProfile', 'changePassword']);
        authService = jasmine.createSpyObj('AuthService', ['checkAuth']);
        router = jasmine.createSpyObj('Router', ['navigate']);
        snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
        dialog = jasmine.createSpyObj('MatDialog', ['open']);

        routeParams$ = new Subject();

        const activatedRouteMock = {
            params: routeParams$.asObservable()
        };

        await TestBed.configureTestingModule({
            imports: [EditProfileComponent],
            providers: [
                { provide: UserService, useValue: userService },
                { provide: AuthService, useValue: authService },
                { provide: Router, useValue: router },
                { provide: ActivatedRoute, useValue: activatedRouteMock },
                { provide: MatSnackBar, useValue: snackBar },
                { provide: MatDialog, useValue: dialog }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(EditProfileComponent);
        component = fixture.componentInstance;
    });

    // ---------- INIT ----------

    it('should create component', () => {
        expect(component).toBeTruthy();
    });

    it('should load user profile on init when userId exists', () => {
        spyOn(component as any, 'loadUserProfile');

        component.ngOnInit();
        routeParams$.next({ userId: 1 });

        expect((component as any).loadUserProfile).toHaveBeenCalledWith(1);
    });

    // ---------- LOAD USER ----------

    it('should load user and patch form', () => {
        userService.getUserDetails.and.returnValue(of(mockUser));

        (component as any).loadUserProfile(1);

        expect(userService.getUserDetails).toHaveBeenCalledWith(1);
        expect(component.user).toEqual(mockUser);
        expect(component.profileForm.value.username).toBe('testuser');
        expect(component.profileForm.value.email).toBe('test@test.com');
        expect(component.profileForm.value.bio).toBe('bio');
    });

    // ---------- PROFILE SUBMIT ----------

    it('should update profile directly when only bio changes', () => {
        component.userId = 1;
        component['originalProfileData'] = {
            username: 'testuser',
            email: 'test@test.com',
            bio: 'old bio'
        };

        component.profileForm.patchValue({
            username: 'testuser',
            email: 'test@test.com',
            bio: 'new bio'
        });

        spyOn(component as any, 'updateProfile');
        spyOn(component as any, 'openConfirmDialog');

        component.onSubmitProfile();

        expect((component as any).updateProfile).toHaveBeenCalled();
        expect((component as any).openConfirmDialog).not.toHaveBeenCalled();
    });

    it('should open confirm dialog when username changes', () => {
        component.userId = 1;
        component['originalProfileData'] = {
            username: 'testuser',
            email: 'test@test.com',
            bio: 'bio'
        };

        component.profileForm.patchValue({
            username: 'newuser',
            email: 'test@test.com',
            bio: 'bio'
        });

        spyOn(component as any, 'openConfirmDialog');

        component.onSubmitProfile();

        expect((component as any).openConfirmDialog).toHaveBeenCalled();
    });

    it('should update profile successfully', () => {
        component.userId = 1;

        component.profileForm.patchValue({
            username: 'testuser',
            email: 'test@test.com',
            bio: 'bio'
        });

        userService.updateProfile.and.returnValue(of(mockUser));
        authService.checkAuth.and.returnValue(of(null));

        (component as any).updateProfile();

        expect(userService.updateProfile).toHaveBeenCalledWith(1, {
            username: 'testuser',
            email: 'test@test.com',
            bio: 'bio'
        });

        expect(authService.checkAuth).toHaveBeenCalled();
        expect(snackBar.open).toHaveBeenCalled();
        expect(router.navigate).toHaveBeenCalledWith(['/users', 1]);
        expect(component.isLoadingProfile).toBeFalse();
    });

    it('should set username error when update profile fails', () => {
        component.userId = 1;

        component.profileForm.patchValue({
            username: 'testuser',
            email: 'test@test.com',
            bio: 'bio'
        });

        userService.updateProfile.and.returnValue(
            throwError(() => ({
                status: 400,
                error: { message: 'Username already taken' }
            }))
        );

        (component as any).updateProfile();

        expect(component.profileErrorMessage).toBe('Username already taken');
        expect(component.isLoadingProfile).toBeFalse();
    });

    // ---------- PASSWORD ----------

    it('should open confirm dialog on valid password submit', () => {
        component.userId = 1;

        component.passwordForm.patchValue({
            oldPassword: 'oldpassword',
            newPassword: 'newpassword123',
            confirmPassword: 'newpassword123'
        });

        spyOn(component as any, 'openConfirmDialog');

        component.onSubmitPassword();

        expect((component as any).openConfirmDialog).toHaveBeenCalled();
    });

    it('should change password successfully', () => {
        component.userId = 1;

        component.passwordForm.patchValue({
            oldPassword: 'oldpassword',
            newPassword: 'newpassword123',
            confirmPassword: 'newpassword123'
        });

        userService.changePassword.and.returnValue(of(void 0));

        (component as any).updatePassword();

        expect(userService.changePassword).toHaveBeenCalledWith(1, {
            oldPassword: 'oldpassword',
            newPassword: 'newpassword123'
        });

        expect(snackBar.open).toHaveBeenCalled();
        expect(router.navigate).toHaveBeenCalledWith(['/users', 1]);
        expect(component.isLoadingPassword).toBeFalse();
    });

    it('should set password error when current password is incorrect', () => {
        component.userId = 1;

        component.passwordForm.patchValue({
            oldPassword: 'wrong',
            newPassword: 'newpassword123',
            confirmPassword: 'newpassword123'
        });

        userService.changePassword.and.returnValue(
            throwError(() => ({
                status: 400,
                error: { message: 'Current password is incorrect' }
            }))
        );

        (component as any).updatePassword();

        expect(component.passwordErrorMessage).toBe('Current password is incorrect');
        expect(component.isLoadingPassword).toBeFalse();
    });

    // ---------- DIALOG ----------

    it('should call callback when dialog is confirmed', () => {
        const callback = jasmine.createSpy('callback');

        dialog.open.and.returnValue({
            afterClosed: () => of(true)
        } as any);

        (component as any).openConfirmDialog('Title', 'Message', callback);

        expect(callback).toHaveBeenCalled();
    });

    // ---------- TOGGLES ----------

    it('should toggle old password visibility', () => {
        component.toggleOldPasswordVisibility();
        expect(component.hideOldPassword).toBeFalse();
    });

    it('should toggle new password visibility', () => {
        component.toggleNewPasswordVisibility();
        expect(component.hideNewPassword).toBeFalse();
    });

    it('should toggle confirm password visibility', () => {
        component.toggleConfirmPasswordVisibility();
        expect(component.hideConfirmPassword).toBeFalse();
    });

    // ---------- DESTROY ----------

    it('should complete destroy$ on ngOnDestroy', () => {
        spyOn(component['destroy$'], 'next');
        spyOn(component['destroy$'], 'complete');

        component.ngOnDestroy();

        expect(component['destroy$'].next).toHaveBeenCalled();
        expect(component['destroy$'].complete).toHaveBeenCalled();
    });

});