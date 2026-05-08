import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../../services/auth.service';

describe('LoginComponent - Unit Tests', () => {
    let component: LoginComponent;
    let fixture: ComponentFixture<LoginComponent>;
    let authService: jasmine.SpyObj<AuthService>;
    let router: Router;

    beforeEach(async () => {
        authService = jasmine.createSpyObj('AuthService', ['login', 'checkAuth']);
        router = jasmine.createSpyObj('Router', ['navigate']);

        await TestBed.configureTestingModule({
            imports: [LoginComponent],
            providers: [
                provideRouter([]),
                { provide: AuthService, useValue: authService },
            ]
        }).compileComponents();

        authService.checkAuth.and.returnValue(of(null));

        router = TestBed.inject(Router);
        spyOn(router, 'navigate');
        fixture = TestBed.createComponent(LoginComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create component', () => {
        expect(component).toBeTruthy();
    });

    it('should call authService.checkAuth on init', () => {
        component.ngOnInit();

        expect(authService.checkAuth).toHaveBeenCalled();
    });

    it('should call authService.login with username and password', () => {
        authService.login.and.returnValue(of({
            id: 1,
            username: 'testuser',
            roles: ['USER']
        }));

        component.username = 'testuser';
        component.password = 'password123';

        component.onSubmit();

        expect(authService.login).toHaveBeenCalledWith('testuser', 'password123');
    });

    it('should navigate to home on successful login', () => {
        authService.login.and.returnValue(of({
            id: 1,
            username: 'testuser',
            roles: ['USER']
        }));

        component.username = 'testuser';
        component.password = 'password123';

        component.onSubmit();

        expect(router.navigate).toHaveBeenCalledWith(['/']);
    });

    it('should show invalid credentials error on 401', () => {
        authService.login.and.returnValue(
            throwError(() => ({ status: 401 }))
        );

        component.username = 'testuser';
        component.password = 'wrongpassword';

        component.onSubmit();

        expect(component.errorMessage).toBe('Invalid username or password');
        expect(router.navigate).not.toHaveBeenCalled();
    });

    it('should show server connection error on status 0', () => {
        authService.login.and.returnValue(
            throwError(() => ({ status: 0 }))
        );

        component.username = 'testuser';
        component.password = 'password';

        component.onSubmit();

        expect(component.errorMessage).toBe('Unable to connect to the server');
    });

    it('should show unexpected error message on unknown status', () => {
        authService.login.and.returnValue(
            throwError(() => ({ status: 500 }))
        );

        component.username = 'testuser';
        component.password = 'password123';

        component.onSubmit();

        expect(component.errorMessage).toBe('Unexpected error. Please try again later');
        expect(router.navigate).not.toHaveBeenCalled();
    });

});