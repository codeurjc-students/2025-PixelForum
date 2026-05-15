import { TestBed } from '@angular/core/testing';
import { HttpClientModule } from '@angular/common/http';

import { UserService } from '../user.service';
import { AuthService } from '../auth.service';

import { User } from '../../models/user.model';
import { ImageService } from '../image.service';

jasmine.DEFAULT_TIMEOUT_INTERVAL = 30000;
jasmine.getEnv().configure({ random: false });

describe('UserService - Integration', () => {

    let service: UserService;
    let authService: AuthService;
    let imageService: ImageService;
    let currentUser: User;

    beforeEach((done: DoneFn) => {
        TestBed.configureTestingModule({
            imports: [HttpClientModule],
            providers: [UserService, AuthService, ImageService]
        });

        service = TestBed.inject(UserService);
        authService = TestBed.inject(AuthService);
        imageService = TestBed.inject(ImageService);
        authService.login('martin', 'user').subscribe({
            next: user => {
                expect(user).toBeTruthy();
                currentUser = user!;
                done();
            },
            error: err => {
                fail('Login failed: ' + err.message);
                done();
            }
        });
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('getAll should fetch paginated users', (done: DoneFn) => {
        service.getAll(0, 10).subscribe({
            next: response => {
                expect(response).toBeTruthy();
                expect(Array.isArray(response.content)).toBeTrue();
                expect(response.number).toBe(0);
                expect(response.size).toBe(10);
                done();
            },
            error: err => {
                fail('Error fetching users: ' + err.message);
                done();
            }
        });
    });

    it('getById should fetch user by id', (done: DoneFn) => {
        service.getById(currentUser.id!).subscribe({
            next: user => {
                expect(user).toBeTruthy();
                expect(user.id).toBe(currentUser.id);
                expect(user.email).toBeUndefined();
                done();
            },
            error: err => {
                fail('Error fetching user: ' + err.message);
                done();
            }
        });
    });

    it('getUserDetails should fetch user details', (done: DoneFn) => {
        service.getUserDetails(currentUser.id!).subscribe({
            next: user => {
                expect(user).toBeTruthy();
                expect(user.id).toBe(currentUser.id);
                expect(user.email).toBe(currentUser.email);
                done();
            },
            error: err => {
                fail('Error fetching user details: ' + err.message);
                done();
            }
        });
    });

    it('getLikedPosts should fetch liked posts', (done: DoneFn) => {
        service.getLikedPosts(currentUser.id!, 0, 10).subscribe({
            next: response => {
                expect(response).toBeTruthy();
                expect(Array.isArray(response.content)).toBeTrue();
                done();
            },
            error: err => {
                fail('Error fetching liked posts: ' + err.message);
                done();
            }
        });
    });

    it('register should create a new user', (done: DoneFn) => {
        const timestamp = Date.now();
        const newUser = {
            username: 'testuser' + timestamp,
            email: `test${timestamp}@mail.com`,
            password: 'password123'
        };

        service.register(newUser).subscribe({
            next: user => {
                expect(user).toBeTruthy();
                expect(user.id).toBeGreaterThan(0);
                expect(user.username).toBe(newUser.username);
                done();
            },
            error: err => {
                fail('Error registering user: ' + err.message);
                done();
            }
        });
    });

    it('register with duplicated username should fail', (done: DoneFn) => {
        const duplicatedUser = {
            username: 'martin',
            email: 'duplicate@mail.com',
            password: 'password123'
        };

        service.register(duplicatedUser).subscribe({
            next: () => {
                fail('Register should fail');
                done();
            },
            error: err => {
                expect(err.status).toBeGreaterThanOrEqual(400);
                done();
            }
        });
    });

    it('updateProfile should update profile', (done: DoneFn) => {
        const updatedData = {
            username: currentUser.username,
            email: currentUser.email || '',
            bio: 'Updated bio ' + Date.now()
        };

        service.updateProfile(currentUser.id!, updatedData).subscribe({
            next: updatedUser => {
                expect(updatedUser.bio).toBe(updatedData.bio);
                done();
            },
            error: err => {
                fail('Error updating profile: ' + err.message);
                done();
            }
        });
    });

    it('should not allow updating another user profile', (done: DoneFn) => {
        authService.login('robert', 'user').subscribe({
            next: () => {
                const updatedData = {
                    username: currentUser.username,
                    email: currentUser.email || '',
                    bio: 'Illegal update'
                };

                service.updateProfile(currentUser.id!, updatedData).subscribe({
                    next: () => {
                        fail('Update should not be allowed');
                        done();
                    },
                    error: err => {
                        expect(err.status).toBe(403);
                        done();
                    }
                });
            },
            error: err => {
                fail('Login failed: ' + err.message);
                done();
            }
        });
    });

    it('changePassword should change password', (done: DoneFn) => {
        const newPassword = 'newpassword123';

        service.changePassword(currentUser.id!, { oldPassword: 'user', newPassword }).subscribe({
            next: () => {
                authService.logout().subscribe({
                    next: () => {
                        authService.login('martin', newPassword).subscribe({
                            next: user => {
                                expect(user).toBeTruthy();
                                // Restore original password
                                service.changePassword(currentUser.id!, { oldPassword: newPassword, newPassword: 'user' }).subscribe({
                                    next: () => done(),
                                    error: err => {
                                        fail('Error restoring password: ' + err.message);
                                        done();
                                    }
                                });
                            },
                            error: err => {
                                fail('Login with new password failed: ' + err.message);
                                done();
                            }
                        });

                    }
                });
            },
            error: err => {
                fail('Error changing password: ' + err.message);
                done();
            }
        });
    });

    it('changePassword with wrong old password should fail', (done: DoneFn) => {
        service.changePassword(currentUser.id!, { oldPassword: 'wrongpassword', newPassword: 'whatever123' }).subscribe({
            next: () => {
                fail('Password change should fail');
                done();
            },
            error: err => {
                expect(err.status).toBe(400);
                done();
            }
        });
    });

    it('setAvatar should assign profile image to user', (done: DoneFn) => {
        const file = new File(['test'], 'avatar.png', { type: 'image/png' });

        // FIRST UPLOAD IMAGE
        imageService.uploadImages([file]).subscribe({
            next: response => {
                const imageId = Number(response[0]);
                service.setAvatar(currentUser.id!, imageId).subscribe({
                    next: updatedUser => {
                        expect(updatedUser).toBeTruthy();
                        expect(updatedUser.avatar).toBeTruthy();
                        done();
                    },
                    error: err => {
                        fail('Error setting avatar: ' + err.message);
                        done();
                    }
                });
            },
            error: err => {
                fail('Image upload failed: ' + err.message);
                done();
            }
        });
    });

    it('setAvatar without permission should fail', (done: DoneFn) => {
        const imageId = 1;

        authService.login('robert', 'user').subscribe({
            next: () => {
                service.setAvatar(currentUser.id!, imageId).subscribe({
                    next: () => {
                        fail('Avatar update should not be allowed');
                        done();
                    },
                    error: err => {
                        expect(err.status).toBe(403);
                        done();
                    }
                });
            },
            error: err => {
                fail('Login failed: ' + err.message);
                done();
            }
        });
    });

    it('deleteAvatar should remove profile image', (done: DoneFn) => {
        const file = new File(['test'], 'avatar.png', { type: 'image/png' });

        // FIRST UPLOAD IMAGE
        imageService.uploadImages([file]).subscribe({
            next: response => {
                const imageId = Number(response[0]);
                // SET AVATAR
                service.setAvatar(currentUser.id!, imageId).subscribe({
                    next: () => {
                        // DELETE AVATAR
                        service.deleteAvatar(currentUser.id!).subscribe({
                            next: () => {
                                service.getUserDetails(currentUser.id!).subscribe({
                                    next: user => {
                                        expect(user.avatar).toBeFalsy();
                                        done();
                                    },
                                    error: err => {
                                        fail('Error fetching user: ' + err.message);
                                        done();
                                    }
                                });
                            },
                            error: err => {
                                fail('Error deleting avatar: ' + err.message);
                                done();
                            }
                        });
                    },
                    error: err => {
                        fail('Error setting avatar: ' + err.message);
                        done();
                    }
                });
            },
            error: err => {
                fail('Image upload failed: ' + err.message);
                done();
            }
        });
    });

    it('deleteAvatar without permission should fail', (done: DoneFn) => {
        authService.login('robert', 'user').subscribe({
            next: () => {
                service.deleteAvatar(currentUser.id!).subscribe({
                    next: () => {
                        fail('Avatar delete should not be allowed');
                        done();
                    },
                    error: err => {
                        expect(err.status).toBe(403);
                        done();
                    }
                });
            },
            error: err => {
                fail('Login failed: ' + err.message);
                done();
            }
        });
    });

    it('deleteAccount should remove the created user', (done: DoneFn) => {
        const timestamp = Date.now();
        const newUser = {
            username: 'deleteuser' + timestamp,
            email: `delete${timestamp}@mail.com`,
            password: 'password123'
        };

        // CREATE USER
        service.register(newUser).subscribe({
            next: createdUser => {
                expect(createdUser.id).toBeTruthy();
                // LOGIN WITH NEW USER
                authService.login(newUser.username, newUser.password).subscribe({
                    next: loggedUser => {
                        expect(loggedUser).toBeTruthy();
                        // DELETE ACCOUNT
                        service.deleteAccount(createdUser.id!).subscribe({
                            next: () => {
                                // TRY LOGIN AGAIN
                                authService.login(newUser.username, newUser.password).subscribe({
                                    next: () => {
                                        fail('Deleted user should not login');
                                        done();
                                    },
                                    error: err => {
                                        expect(err.status).toBe(401);
                                        done();
                                    }
                                });
                            },
                            error: err => {
                                fail('Error deleting account: ' + err.message);
                                done();
                            }
                        });
                    },
                    error: err => {
                        fail('Login failed: ' + err.message);
                        done();
                    }
                });
            },
            error: err => {
                fail('Error creating user: ' + err.message);
                done();
            }
        });
    });

    it('deleteAccount without permission should fail', (done: DoneFn) => {
        authService.login('robert', 'user').subscribe({
            next: () => {
                service.deleteAccount(currentUser.id!).subscribe({
                    next: () => {
                        fail('Delete should not be allowed');
                        done();
                    },
                    error: err => {
                        expect(err.status).toBe(403);
                        done();
                    }
                });
            },
            error: err => {
                fail('Login failed: ' + err.message);
                done();
            }
        });
    });

    it('getUserDetails without authentication should fail', (done: DoneFn) => {
        authService.logout().subscribe({
            next: () => {
                service.getUserDetails(currentUser.id!).subscribe({
                    next: () => {
                        fail('Request should fail');
                        done();
                    },
                    error: err => {
                        expect(err.status).toBe(401);
                        done();
                    }
                });
            },
            error: err => {
                fail('Logout failed: ' + err.message);
                done();
            }
        });
    });

    it('deleteAccount without authentication should fail', (done: DoneFn) => {
        authService.logout().subscribe({
            next: () => {
                service.deleteAccount(currentUser.id!).subscribe({
                    next: () => {
                        fail('Delete should fail');
                        done();
                    },
                    error: err => {
                        expect(err.status).toBe(401);
                        done();
                    }
                });
            },
            error: err => {
                fail('Logout failed: ' + err.message);
                done();
            }
        });
    });

});