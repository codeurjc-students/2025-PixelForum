import { TestBed } from '@angular/core/testing';
import { HttpClientModule } from '@angular/common/http';
import { AuthService } from '../auth.service';

jasmine.DEFAULT_TIMEOUT_INTERVAL = 30000;
jasmine.getEnv().configure({ random: false });

describe('AuthService - Integration Tests', () => {
    let service: AuthService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientModule],
            providers: [AuthService]
        });
        service = TestBed.inject(AuthService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('login should connect to real API and return response', (done: DoneFn) => {
        service.login('admin', 'admin0').subscribe({
            next: response => {
                expect(response).toBeTruthy();
                expect(response?.username).toBe('admin');
                done();
            },
            error: err => {
                fail('Login failed: ' + err.message);
                done();
            }
        });
    });

    it('login with invalid credentials should fail', (done: DoneFn) => {
        service.login('invalid', 'wrong').subscribe({
            next: () => {
                fail('Should have failed with invalid credentials');
                done();
            },
            error: err => {
                expect(err.status).toBe(401);
                done();
            }
        });
    });

    it('me should return user info when authenticated', (done: DoneFn) => {
        // First login
        service.login('admin', 'admin0').subscribe({
            next: () => {
                // Then get user info
                service.me().subscribe({
                    next: user => {
                        expect(user).toBeTruthy();
                        expect(user.username).toBe('admin');
                        done();
                    },
                    error: err => {
                        fail('Me endpoint failed: ' + err.message);
                        done();
                    }
                });
            }
        });
    });

    it('checkAuth should update loggedIn$ observable if authenticated', (done: DoneFn) => {
        // Login first
        service.login('admin', 'admin0').subscribe({
            next: () => {
                // Check auth
                service.checkAuth().subscribe({
                    next: () => {
                        service.loggedIn$.subscribe(isLoggedIn => {
                            expect(isLoggedIn).toBe(true);
                            done();
                        });
                    }
                });
            }
        });
    });

    it('logout should clear authentication', (done: DoneFn) => {
        // Login first
        service.login('admin', 'admin0').subscribe({
            next: () => {
                // Then logout
                service.logout().subscribe({
                    next: () => {
                        service.loggedIn$.subscribe(isLoggedIn => {
                            expect(isLoggedIn).toBe(false);
                            done();
                        });
                    },
                    error: err => {
                        fail('Logout failed: ' + err.message);
                        done();
                    }
                });
            }
        });
    });

    it('logout without login should keep loggedIn$ false', (done: DoneFn) => {
        service.logout().subscribe({
            next: () => {
                service.loggedIn$.subscribe(isLoggedIn => {
                    expect(isLoggedIn).toBe(false);
                    done();
                });
            },
            error: err => {
                fail('Logout failed unexpectedly: ' + err.message);
                done();
            }
        });
    });
});