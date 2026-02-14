import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject, of } from 'rxjs';
import { HeaderComponent } from './header.component';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { RouterTestingModule } from '@angular/router/testing';

describe('HeaderComponent - Unit Tests', () => {
    let component: HeaderComponent;
    let fixture: ComponentFixture<HeaderComponent>;
    let authService: jasmine.SpyObj<AuthService>;
    let loggedInSubject: BehaviorSubject<boolean>;

    beforeEach(async () => {
        loggedInSubject = new BehaviorSubject<boolean>(true);

        // Mock AuthService
        authService = jasmine.createSpyObj('AuthService', ['checkAuth', 'logout'], {
            loggedIn$: loggedInSubject.asObservable()
        });

        authService.checkAuth.and.returnValue(of(null));
        authService.logout.and.returnValue(of({}));

        await TestBed.configureTestingModule({
            imports: [
                HeaderComponent,
                RouterTestingModule,
                CommonModule
            ],
            providers: [
                { provide: AuthService, useValue: authService }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(HeaderComponent);
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

    it('should call authService.logout when onLogout is called', () => {
        component.onLogout();
        expect(authService.logout).toHaveBeenCalled();
    });

    it('should render logout button when logged in', () => {
        fixture.detectChanges();
        const compiled = fixture.nativeElement as HTMLElement;
        const logoutBtn = compiled.querySelector('#logout-button');
        expect(logoutBtn).toBeTruthy();
    });

    it('should not render logout button when not logged in', () => {
        loggedInSubject.next(false);
        fixture.detectChanges();

        const compiled = fixture.nativeElement as HTMLElement;
        const logoutBtn = compiled.querySelector('#logout-button');
        expect(logoutBtn).toBeNull();
    });
});