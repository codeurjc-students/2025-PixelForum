import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from '../auth.service';
import { environment } from '../../../environments/environment';

describe('AuthService - Unit Tests', () => {
    let service: AuthService;
    let httpMock: HttpTestingController;
    const baseUrl = environment.apiUrl + 'auth/';

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [AuthService]
        });
        service = TestBed.inject(AuthService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('login should send POST with withCredentials', () => {
        const username = 'testuser';
        const password = 'password123';
        const mockResponse = { status: 'SUCCESS' };

        service.login(username, password).subscribe();

        const req = httpMock.expectOne(baseUrl + 'login');
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({ username, password });
        expect(req.request.withCredentials).toBe(true);
        
        req.flush(mockResponse);
    });

    it('should set loggedIn$ to true on successful login', (done) => {
		const mockResponse = { status: 'SUCCESS', message: '', error: null };

		service.login('user', 'pass').subscribe(() => {
			service.loggedIn$.subscribe(isLoggedIn => {
				expect(isLoggedIn).toBeTrue();
				done();
			});
		});

		const req = httpMock.expectOne(baseUrl + 'login');
		expect(req.request.method).toBe('POST');
		req.flush(mockResponse);
	});

	it('should NOT set loggedIn$ to true on failed login', (done) => {
		const mockResponse = { status: 'FAILURE', message: 'Bad credentials', error: 'BAD_CREDENTIALS' };

		service.login('user', 'wrongpass').subscribe(() => {
			service.loggedIn$.subscribe(isLoggedIn => {
				expect(isLoggedIn).toBeFalse();
				done();
			});
		});

		const req = httpMock.expectOne(baseUrl + 'login');
		expect(req.request.method).toBe('POST');
		req.flush(mockResponse);
	});

	it('should handle HTTP error correctly', (done) => {
		service.login('user', 'wrongpass').subscribe({
			error: () => {
				service.loggedIn$.subscribe(isLoggedIn => {
					expect(isLoggedIn).toBeFalse();
					done();
				});
			}
		});

		const req = httpMock.expectOne(baseUrl + 'login');
		req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
	});

    it('logout should send POST with withCredentials', () => {
        const mockResponse = { status: 'SUCCESS' };

        service.logout().subscribe();

        const req = httpMock.expectOne(baseUrl + 'logout');
        expect(req.request.method).toBe('POST');
        expect(req.request.withCredentials).toBe(true);
        
        req.flush(mockResponse);
    });

    it('logout should update loggedIn$ to false', (done) => {
        const mockResponse = { status: 'SUCCESS' };

        service.logout().subscribe(() => {
            service.loggedIn$.subscribe(isLoggedIn => {
                expect(isLoggedIn).toBe(false);
                done();
            });
        });

        const req = httpMock.expectOne(baseUrl + 'logout');
        req.flush(mockResponse);
    });

    it('me should send GET with withCredentials', () => {
        const mockUser = { username: 'testuser' };

        service.me().subscribe();

        const req = httpMock.expectOne(baseUrl + 'me');
        expect(req.request.method).toBe('GET');
        expect(req.request.withCredentials).toBe(true);
        
        req.flush(mockUser);
    });

    it('checkAuth should set loggedIn to true when me succeeds', (done) => {
        const mockUser = { username: 'testuser' };

        service.checkAuth().subscribe(() => {
            service.loggedIn$.subscribe(isLoggedIn => {
                expect(isLoggedIn).toBe(true);
                done();
            });
        });

        const req = httpMock.expectOne(baseUrl + 'me');
        req.flush(mockUser);
    });

    it('checkAuth should set loggedIn to false when me fails', (done) => {
        service.checkAuth().subscribe(() => {
            service.loggedIn$.subscribe(isLoggedIn => {
                expect(isLoggedIn).toBe(false);
                done();
            });
        });

        const req = httpMock.expectOne(baseUrl + 'me');
        req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });

    it('checkAuth should emit null when me fails', (done) => {
        service.checkAuth().subscribe(result => {
            expect(result).toBeNull();
            done();
        });

        const req = httpMock.expectOne(baseUrl + 'me');
        req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });
});