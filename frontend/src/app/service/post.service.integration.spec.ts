import { TestBed } from '@angular/core/testing';
import { HttpClientModule } from '@angular/common/http';
import { PostService } from './post.service';
import { Post } from '../models/post.model';

jasmine.DEFAULT_TIMEOUT_INTERVAL = 30000;
jasmine.getEnv().configure({ random: false });

describe('PostService', () => {
  let service: PostService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientModule],
      providers: [PostService]
    });
    service = TestBed.inject(PostService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getAll should fetch posts from the real API', (done: DoneFn) => {
    service.getAll().subscribe({
      next: posts => {
        expect(posts).toBeTruthy();
        expect(Array.isArray(posts)).toBeTrue();
        expect(posts.length).toBeGreaterThan(0);
        done();
      },
      error: err => {
        fail('Error connecting to the API: ' + err.message);
        done();
      }
    });
  });
});