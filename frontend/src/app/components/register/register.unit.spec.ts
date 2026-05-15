import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RegisterComponent } from './register.component';
import { UserService } from '../../services/user.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideRouter, Router } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';

describe('RegisterComponent - Unit Tests', () => {

    let component: RegisterComponent;
    let fixture: ComponentFixture<RegisterComponent>;

    let userService: jasmine.SpyObj<UserService>;
    let snackBar: jasmine.SpyObj<MatSnackBar>;
    let router: Router;

    beforeEach(async () => {
        userService = jasmine.createSpyObj('UserService', ['register']);
        snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
        router = jasmine.createSpyObj('Router', ['navigate']);

        await TestBed.configureTestingModule({
            imports: [RegisterComponent, ReactiveFormsModule],
            providers: [
                provideRouter([]),
                { provide: UserService, useValue: userService },
                { provide: MatSnackBar, useValue: snackBar }
            ]
        }).compileComponents();

        router = TestBed.inject(Router);
        spyOn(router, 'navigate');
        fixture = TestBed.createComponent(RegisterComponent);
        component = fixture.componentInstance;
    });

    // ---------- INIT ----------
    it('should create component', () => {
        expect(component).toBeTruthy();
    });

    // -------- Validations --------

    it('should be invalid when empty', () => {
        expect(component.registerForm.valid).toBeFalse();
    });

    it('should validate password mismatch', () => {
        component.registerForm.patchValue({
            password: '12345678',
            confirmPassword: 'different'
        });

        const confirm = component.registerForm.get('confirmPassword');

        expect(confirm?.hasError('passwordMismatch')).toBeTrue();
    });

    it('should validate password match correctly', () => {
        component.registerForm.patchValue({
            password: '12345678',
            confirmPassword: '12345678'
        });

        const confirm = component.registerForm.get('confirmPassword');

        expect(confirm?.hasError('passwordMismatch')).toBeFalse();
    });

    // ------- Toggle Visibility -------

    it('should toggle password visibility', () => {
        const initial = component.hidePassword;

        component.togglePasswordVisibility();

        expect(component.hidePassword).toBe(!initial);
    });

    it('should toggle confirm password visibility', () => {
        const initial = component.hideConfirmPassword;

        component.toggleConfirmPasswordVisibility();

        expect(component.hideConfirmPassword).toBe(!initial);
    });

    // --------- On Submit ---------

    it('should register successfully', () => {
        component.registerForm.setValue({
            username: 'test',
            email: 'test@test.com',
            password: '12345678',
            confirmPassword: '12345678',
            bio: 'bio'
        });

        userService.register.and.returnValue(of({ id: 1, username: 'test' } as any));

        component.onSubmit();

        expect(userService.register).toHaveBeenCalled();
        expect(snackBar.open).toHaveBeenCalled();
        expect(router.navigate).toHaveBeenCalledWith(['/login']);
        expect(component.isLoading).toBeFalse();
    });

    it('should not submit if form is invalid', () => {
        component.onSubmit();

        expect(userService.register).not.toHaveBeenCalled();
    });

    it('should set backend error message for status 400', () => {
        component.registerForm.setValue({
            username: 'test',
            email: 'test@test.com',
            password: '12345678',
            confirmPassword: '12345678',
            bio: ''
        });

        userService.register.and.returnValue(
            throwError(() => ({
                status: 400,
                error: { message: 'Username already exists' }
            }))
        );

        component.onSubmit();

        expect(component.isLoading).toBeFalse();
        expect(component.errorMessage).toBe('Username already exists');
    });

    it('should send null bio when empty', () => {
        component.registerForm.setValue({
            username: 'test',
            email: 'test@test.com',
            password: '12345678',
            confirmPassword: '12345678',
            bio: '   '
        });

        userService.register.and.returnValue(of());

        component.onSubmit();

        expect(userService.register).toHaveBeenCalledWith(jasmine.objectContaining({
            bio: null
        }));
    });

    it('should clear password mismatch after fixing confirmation', () => {
        component.registerForm.patchValue({
            password: '12345678',
            confirmPassword: 'wrong'
        });

        let confirm = component.registerForm.get('confirmPassword');
        expect(confirm?.hasError('passwordMismatch')).toBeTrue();

        component.registerForm.patchValue({
            confirmPassword: '12345678'
        });

        confirm = component.registerForm.get('confirmPassword');
        expect(confirm?.hasError('passwordMismatch')).toBeFalse();
    });

    it('should return bio maxlength error', () => {
        const field = component.registerForm.get('bio');
        field?.setErrors({ maxlength: true });

        expect(component.getErrorMessage('bio'))
            .toBe('Bio must not exceed 255 characters');
    });

    // --------- Form Errors ---------

    it('should return username required error', () => {
        const field = component.registerForm.get('username');
        field?.setErrors({ required: true });

        expect(component.getErrorMessage('username')).toBe('Please enter your username');
    });

    it('should return email invalid error', () => {
        const field = component.registerForm.get('email');
        field?.setErrors({ email: true });

        expect(component.getErrorMessage('email')).toBe('Please enter a valid email');
    });

    it('should return password minlength error', () => {
        const field = component.registerForm.get('password');
        field?.setErrors({ minlength: true });

        expect(component.getErrorMessage('password')).toBe('Password must be at least 8 characters');
    });

    it('should return confirm password mismatch error', () => {
        const field = component.registerForm.get('confirmPassword');
        field?.setErrors({ passwordMismatch: true });

        expect(component.getErrorMessage('confirmPassword')).toBe('Passwords do not match');
    });

    // --------- isFieldInvalid ---------

    it('should return true if field is invalid and touched', () => {
        const field = component.registerForm.get('username');

        field?.setErrors({ required: true });
        field?.markAsTouched();

        expect(component.isFieldInvalid('username')).toBeTrue();
    });

    it('should return false if field is valid', () => {
        const field = component.registerForm.get('username');

        field?.setErrors(null);

        expect(component.isFieldInvalid('username')).toBeFalse();
    });

});