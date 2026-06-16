import { TestBed } from '@angular/core/testing';
import { HttpClientModule } from '@angular/common/http';
import { CommentService } from '../comment.service';
import { AuthService } from '../auth.service';
import { Comment } from '../../models/comment.model';

jasmine.DEFAULT_TIMEOUT_INTERVAL = 30000;
jasmine.getEnv().configure({ random: false });

describe('CommentService - Integration', () => {

	let service: CommentService;
	let authService: AuthService;

	const TEST_POST_ID = 1;
	let createdCommentId: number;

	beforeEach((done: DoneFn) => {
		TestBed.configureTestingModule({
			imports: [HttpClientModule],
			providers: [CommentService, AuthService]
		});

		service = TestBed.inject(CommentService);
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

	// ── getPostComments ───────────────────────────────────────────────────────

	it('getPostComments should fetch comments from the real API', (done: DoneFn) => {
		service.getPostComments(TEST_POST_ID).subscribe({
			next: response => {
				expect(response).toBeTruthy();
				expect(Array.isArray(response.content)).toBeTrue();
				done();
			},
			error: err => {
				fail('Error fetching comments: ' + err.message);
				done();
			}
		});
	});

	// ── create ────────────────────────────────────────────────────────────────

	it('create should add a new comment', (done: DoneFn) => {
		const newComment: Comment = { content: 'Integration test comment ' + Date.now() };

		service.create(TEST_POST_ID, newComment).subscribe({
			next: comment => {
				expect(comment).toBeTruthy();
				expect(comment.id).toBeGreaterThan(0);
				expect(comment.content).toBe(newComment.content);
				createdCommentId = comment.id!;
				done();
			},
			error: err => {
				fail('Error creating comment: ' + err.message);
				done();
			}
		});
	});

	it('create without credentials should fail', (done: DoneFn) => {
		authService.logout().subscribe({
			next: () => {
				const newComment: Comment = { content: 'Unauthorized comment ' + Date.now() };

				service.create(TEST_POST_ID, newComment).subscribe({
					next: () => {
						fail('Comment creation should not succeed without authentication');
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

	// ── getById ───────────────────────────────────────────────────────────────

	it('getById should fetch the created comment', (done: DoneFn) => {
		service.getById(TEST_POST_ID, createdCommentId).subscribe({
			next: comment => {
				expect(comment).toBeTruthy();
				expect(comment.id).toBe(createdCommentId);
				done();
			},
			error: err => {
				fail('Error fetching comment: ' + err.message);
				done();
			}
		});
	});

	it('getById should return 404 for non-existing comment', (done: DoneFn) => {
		service.getById(TEST_POST_ID, -1).subscribe({
			next: () => {
				fail('Should have returned 404');
				done();
			},
			error: err => {
				expect(err.status).toBe(404);
				done();
			}
		});
	});

	// ── update ────────────────────────────────────────────────────────────────

	it('update should modify the created comment', (done: DoneFn) => {
		const updatedContent = 'Updated content ' + Date.now();

		service.getById(TEST_POST_ID, createdCommentId).subscribe({
			next: comment => {
				const updated: Comment = { ...comment, content: updatedContent };
				service.update(TEST_POST_ID, createdCommentId, updated).subscribe({
					next: result => {
						expect(result.content).toBe(updatedContent);
						done();
					},
					error: err => {
						fail('Error updating comment: ' + err.message);
						done();
					}
				});
			},
			error: err => {
				fail('Error fetching comment for update: ' + err.message);
				done();
			}
		});
	});

	it('should not allow updating a comment without permission', (done: DoneFn) => {
		service.getById(TEST_POST_ID, createdCommentId).subscribe({
			next: comment => {
				authService.login('robert', 'user').subscribe({
					next: () => {
						const updated: Comment = { ...comment, content: 'Illegal update' };
						service.update(TEST_POST_ID, createdCommentId, updated).subscribe({
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

	// ── toggleLike ────────────────────────────────────────────────────────────

	it('toggleLike should add like to a comment', (done: DoneFn) => {
		const newComment: Comment = { content: 'Like test ' + Date.now() };

		service.create(TEST_POST_ID, newComment).subscribe({
			next: comment => {
				authService.login('robert', 'user').subscribe({
					next: () => {
						service.toggleLike(TEST_POST_ID, comment.id!).subscribe({
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
						fail('Login as robert failed: ' + err.message);
						done();
					}
				});
			},
			error: err => {
				fail('Error creating comment: ' + err.message);
				done();
			}
		});
	});

	it('toggleLike should remove like when already liked', (done: DoneFn) => {
		const newComment: Comment = { content: 'Unlike test ' + Date.now() };

		service.create(TEST_POST_ID, newComment).subscribe({
			next: comment => {
				authService.login('robert', 'user').subscribe({
					next: () => {
						service.toggleLike(TEST_POST_ID, comment.id!).subscribe({
							next: () => {
								service.toggleLike(TEST_POST_ID, comment.id!).subscribe({
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
			}
		});
	});

	it('toggleLike without authentication should fail', (done: DoneFn) => {
		authService.logout().subscribe({
			next: () => {
				service.toggleLike(TEST_POST_ID, createdCommentId).subscribe({
					next: () => {
						fail('Like should not be allowed without auth');
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

	// ── delete ────────────────────────────────────────────────────────────────

	it('should not allow deleting a comment without permission', (done: DoneFn) => {
		authService.login('robert', 'user').subscribe({
			next: () => {
				service.delete(TEST_POST_ID, createdCommentId).subscribe({
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

	it('delete should remove the created comment', (done: DoneFn) => {
		authService.login('martin', 'user').subscribe({
			next: () => {
				service.delete(TEST_POST_ID, createdCommentId).subscribe({
					next: () => {
						service.getById(TEST_POST_ID, createdCommentId).subscribe({
							next: () => {
								fail('Comment was not deleted');
								done();
							},
							error: err => {
								expect(err.status).toBe(404);
								done();
							}
						});
					},
					error: err => {
						fail('Error deleting comment: ' + err.message);
						done();
					}
				});
			}
		});
	});

	// ── getUserComments ───────────────────────────────────────────────────────

	it('getUserComments should fetch comments authored by a user', (done: DoneFn) => {
		service.getUserComments(2).subscribe({
			next: response => {
				expect(response).toBeTruthy();
				expect(Array.isArray(response.content)).toBeTrue();
				done();
			},
			error: err => {
				fail('Error fetching user comments: ' + err.message);
				done();
			}
		});
	});

	// ── getLikedComments ──────────────────────────────────────────────────────

	it('getLikedComments should fetch comments liked by a user', (done: DoneFn) => {
		service.getLikedComments(2).subscribe({
			next: response => {
				expect(response).toBeTruthy();
				expect(Array.isArray(response.content)).toBeTrue();
				done();
			},
			error: err => {
				fail('Error fetching liked comments: ' + err.message);
				done();
			}
		});
	});

});