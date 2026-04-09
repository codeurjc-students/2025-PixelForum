import { TestBed } from '@angular/core/testing';
import { HttpClientModule } from '@angular/common/http';
import { PostService } from '../post.service';
import { Post } from '../../models/post.model';
import { AuthService } from '../auth.service';

jasmine.DEFAULT_TIMEOUT_INTERVAL = 30000;
jasmine.getEnv().configure({ random: false });

describe('PostService - Integration', () => {

	let service: PostService;
	let authService: AuthService;
	let createdPostId: number;

	beforeEach((done: DoneFn) => {

		TestBed.configureTestingModule({
			imports: [HttpClientModule],
			providers: [PostService, AuthService]
		});

		service = TestBed.inject(PostService);
		authService = TestBed.inject(AuthService);

		authService.login('martin', 'user').subscribe({
			next: () => done(),
			error: err => {
				fail('Login failed: ' + err.message);
				done();
			}
		});
	});

	it('should be created', () => {
		expect(service).toBeTruthy();
	});

	it('getPosts should fetch posts from the real API', (done: DoneFn) => {
		service.getPosts().subscribe({
			next: response => {
				expect(response).toBeTruthy();
				expect(Array.isArray(response.content)).toBeTrue();
				done();
			},
			error: err => {
				fail('Error fetching posts: ' + err.message);
				done();
			}
		});
	});

	it('create should add a new post', (done: DoneFn) => {
		const newPost: Post = {
			title: 'Test Post ' + Date.now(),
			content: 'This is a test post',
			topic: { id: 1 } as any,
			images: []
		};

		service.create(newPost).subscribe({
			next: post => {
				expect(post).toBeTruthy();
				expect(post.id).toBeGreaterThan(0);
				createdPostId = post.id!;
				done();
			},
			error: err => {
				fail('Error creating post: ' + err.message);
				done();
			}
		});
	});

	it('getById should fetch the created post', (done: DoneFn) => {
		service.getById(createdPostId).subscribe({
			next: post => {
				expect(post).toBeTruthy();
				expect(post.id).toBe(createdPostId);
				done();
			},
			error: err => {
				fail('Error fetching post: ' + err.message);
				done();
			}
		});

	});

	it('update should modify the created post', (done: DoneFn) => {
		const updatedTitle = 'Updated Title ' + Date.now();

		service.getById(createdPostId).subscribe({
			next: post => {
				post.title = updatedTitle;
				service.update(post).subscribe({
					next: updatedPost => {
						expect(updatedPost.title).toBe(updatedTitle);
						done();
					},
					error: err => {
						fail('Error updating post: ' + err.message);
						done();
					}
				});
			},
			error: err => {
				fail('Error fetching post for update: ' + err.message);
				done();
			}
		});
	});

	it('should not allow updating a post without permission', (done: DoneFn) => {
		service.getById(createdPostId).subscribe({
			next: post => {
				authService.login('robert', 'user').subscribe({
					next: () => {
						post.title = 'Illegal update';
						service.update(post).subscribe({
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
			}
		});
	});

	it('should not allow deleting a post without permission', (done: DoneFn) => {
		service.getById(createdPostId).subscribe({
			next: post => {
				authService.login('robert', 'user').subscribe({
					next: () => {
						service.delete(post.id!).subscribe({
							next: () => {
								fail('Delete should not be allowed');
								done();
							},
							error: err => {
								expect(err.status).toBe(403);
								done();
							}
						});
					}
				});
			}
		});
	});

	it('delete should remove the created post', (done: DoneFn) => {
		service.delete(createdPostId).subscribe({
			next: () => {
				service.getById(createdPostId).subscribe({
					next: () => {
						fail('Post was not deleted');
						done();
					},
					error: error => {
						expect(error.status).toBe(500);
						done();
					}
				});
			},
			error: err => {
				fail('Error deleting post: ' + err.message);
				done();
			}
		});
	});

	it('create without credentials should fail', (done: DoneFn) => {
		authService.logout().subscribe({
			next: () => {
				const newPost: Post = {
					title: 'Unauthorized Post ' + Date.now(),
					content: 'This should not be created',
					topic: { id: 1 } as any,
					images: []
				};

				service.create(newPost).subscribe({
					next: () => {
						fail('Post creation should not succeed without authentication');
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

	it('toggleLike should add like to a post', (done: DoneFn) => {
		const newPost: Post = {
			title: 'Like Test ' + Date.now(),
			content: 'Testing like',
			topic: { id: 1 } as any,
			images: []
		};

		// Create a new post to like
		service.create(newPost).subscribe({
			next: post => {
				expect(post.id).toBeTruthy();

				// WHEN
				service.toggleLike(post.id!).subscribe({
					next: updated => {
						expect(updated.likes).toBe(1);
						done();
					},
					error: err => {
						fail('Error toggling like: ' + err.message);
						done();
					}
				});
			},
			error: err => {
				fail('Error creating post: ' + err.message);
				done();
			}
		});
	});

	it('toggleLike should remove like when already liked', (done: DoneFn) => {
		const newPost: Post = {
			title: 'Unlike Test ' + Date.now(),
			content: 'Testing unlike',
			topic: { id: 1 } as any,
			images: []
		};

		service.create(newPost).subscribe({
			next: post => {
				// First like
				service.toggleLike(post.id!).subscribe({
					next: () => {
						// Then unlike
						service.toggleLike(post.id!).subscribe({
							next: updated => {
								expect(updated.likes).toBe(0);
								done();
							},
							error: err => {
								fail('Error removing like: ' + err.message);
								done();
							}
						});

					}
				});
			}
		});
	});

	it('toggleLike without authentication should fail', (done: DoneFn) => {
		const newPost: Post = {
			title: 'No Auth Like ' + Date.now(),
			content: 'Test',
			topic: { id: 1 } as any,
			images: []
		};

		// Create a new post to like
		service.create(newPost).subscribe({
			next: post => {

				// LOGOUT
				authService.logout().subscribe({
					next: () => {
						// THEN TRY TO LIKE
						service.toggleLike(post.id!).subscribe({
							next: () => {
								fail('Like should not be allowed without auth');
								done();
							},
							error: err => {
								expect(err.status).toBe(401);
								done();
							}
						});

					}
				});

			}
		});
	});

});