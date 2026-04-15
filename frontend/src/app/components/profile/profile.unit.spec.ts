import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { ProfileComponent } from './profile.component';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { ErrorService } from '../../services/error.service';
import { ActivatedRoute, Router } from '@angular/router';
import { User } from '../../models/user.model';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('ProfileComponent - Unit Tests', () => {

    let component: ProfileComponent;
    let fixture: ComponentFixture<ProfileComponent>;
    let userService: jasmine.SpyObj<UserService>;
    let authService: jasmine.SpyObj<AuthService>;
    let errorService: jasmine.SpyObj<ErrorService>;
    let router: jasmine.SpyObj<Router>;

    const mockUser: User = {
        id: 1,
        username: 'testuser'
    } as any;

    let routeParams$: Subject<any>;
    let authUser$: Subject<any>;

    beforeEach(async () => {
        spyOn(console, 'error');

        userService = jasmine.createSpyObj('UserService', ['getById']);
        errorService = jasmine.createSpyObj('ErrorService', ['setError']);
        router = jasmine.createSpyObj('Router', ['navigate']);

        // Subjects to control observables in tests
        routeParams$ = new Subject();
        authUser$ = new Subject();

        authService = {
            user$: authUser$.asObservable()
        } as any;

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
                { provide: ActivatedRoute, useValue: activatedRouteMock }
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
        routeParams$.next({ userId: '1' });

        expect(component.loadUserProfile).toHaveBeenCalledWith(1);
    });

    it('should set error and navigate if no userId in params', () => {
        component.ngOnInit();

        authUser$.next({ id: 1 });
        routeParams$.next({}); // No userId

        expect(errorService.setError).toHaveBeenCalledWith(400, 'Bad Request');
        expect(router.navigate).toHaveBeenCalledWith(['/error']);
    });

    // ---------- LOAD USER PROFILE ----------

    it('should load user and update state correctly', () => {
        userService.getById.and.returnValue(of(mockUser));

        component.loadUserProfile(1);

        expect(userService.getById).toHaveBeenCalledWith(1);
        expect(component.user).toEqual(mockUser);
        expect(component.filterUsername).toBe('testuser');
        expect(component.isLoadingUser).toBeFalse();
    });

    it('should set isOwnProfile true when user is the same as current user', () => {
        userService.getById.and.returnValue(of(mockUser));

        component['currentUserId'] = 1;
        component.loadUserProfile(1);

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
        component.editProfile();

        expect(router.navigate).toHaveBeenCalledWith(['/profile/edit']);
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