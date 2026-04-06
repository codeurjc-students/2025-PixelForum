import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HTTP_INTERCEPTORS, HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { ErrorService } from '../error.service';
import { AuthService } from '../auth.service';
import { ErrorInterceptor } from '../../interceptors/error.interceptor';


describe('ErrorService/Interceptor - Integration Tests', () => {

    let http: HttpClient;
    let httpMock: HttpTestingController;

    let routerSpy: jasmine.SpyObj<Router>;
    let authServiceSpy: jasmine.SpyObj<AuthService>;
    let errorService: ErrorService;

    beforeEach(() => {
        routerSpy = jasmine.createSpyObj('Router', ['navigate']);
        authServiceSpy = jasmine.createSpyObj('AuthService', ['logout']);

        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                ErrorService,
                { provide: Router, useValue: routerSpy },
                { provide: AuthService, useValue: authServiceSpy },
                {
                    provide: HTTP_INTERCEPTORS,
                    useClass: ErrorInterceptor,
                    multi: true
                }
            ]
        });

        http = TestBed.inject(HttpClient);
        httpMock = TestBed.inject(HttpTestingController);
        errorService = TestBed.inject(ErrorService);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should handle 404 error and redirect to error page', () => {
        http.get('/test').subscribe({
            error: () => { }
        });

        const req = httpMock.expectOne('/test');

        req.flush({ error: 'Not found' }, { status: 404, statusText: 'Not Found' });

        const error = errorService.getError();

        expect(error?.status).toBe(404);
        expect(error?.errorName).toBe('Page not found');
        expect(routerSpy.navigate).toHaveBeenCalledWith(['/error']);
    });

    it('should logout and redirect on 401', () => {
        http.get('/test').subscribe({
            error: () => { }
        });

        const req = httpMock.expectOne('/test');

        req.flush({}, { status: 401, statusText: 'Unauthorized' });

        expect(authServiceSpy.logout).toHaveBeenCalled();
        expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
    });

    it('should ignore /me endpoint', () => {
        http.get('/me').subscribe({
            error: () => { }
        });

        const req = httpMock.expectOne('/me');

        req.flush({}, { status: 401, statusText: 'Unauthorized' });

        expect(authServiceSpy.logout).not.toHaveBeenCalled();
        expect(routerSpy.navigate).not.toHaveBeenCalled();
    });

});