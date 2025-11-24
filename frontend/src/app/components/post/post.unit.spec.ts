import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PostComponent } from './post.component';
import { Post } from '../../models/post.model';

describe('PostComponent', () => {
  let fixture: ComponentFixture<PostComponent>;
  let compiled: HTMLElement;
  let component: PostComponent;

  // Mock of a Post object
    const mockPost: Post = {
      id: 1,
      title: 'GTA VI Massive Leak',
      content: 'A massive leak has revealed details...',
      topics: [
        { id: 1, name: 'GTA VI' } as any
      ],
    };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PostComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(PostComponent);
    component = fixture.componentInstance;
    component.post = mockPost;
    fixture.detectChanges();
    compiled = fixture.nativeElement; 
  });

  it('should charge posts', () => {

    // Title
    const title = compiled.querySelector('#post-title');
    expect(title?.textContent).toContain(mockPost.title);

    // Content
    expect(compiled.textContent).toContain(mockPost.content);

    // Topics
    expect(compiled.textContent).toContain('#GTA VI');
  });
});
