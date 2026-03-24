import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CreatePostComponent } from './create-post.component';
import { FormBuilder } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { PostService } from '../../services/post.service';
import { TopicService } from '../../services/topic.service';
import { ImageService } from '../../services/image.service';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Topic } from '../../models/topic.model';
import { Post } from '../../models/post.model';

describe('CreatePostComponent', () => {

    let component: CreatePostComponent;
    let fixture: ComponentFixture<CreatePostComponent>;

    let postServiceSpy: jasmine.SpyObj<PostService>;
    let topicServiceSpy: jasmine.SpyObj<TopicService>;
    let imageServiceSpy: jasmine.SpyObj<ImageService>;
    let routerSpy: jasmine.SpyObj<Router>;
    let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

    const mockTopic: Topic = {
        id: 1,
        name: 'Angular'
    } as Topic;

    const mockPost: Post = {
        id: 1,
        title: 'Test Post',
        content: 'Test Content',
        topic: mockTopic,
        images: []
    };

    beforeEach(async () => {
        postServiceSpy = jasmine.createSpyObj('PostService', ['create', 'update', 'getById']);
        topicServiceSpy = jasmine.createSpyObj('TopicService', ['getAll']);
        imageServiceSpy = jasmine.createSpyObj('ImageService', ['uploadImages']);
        routerSpy = jasmine.createSpyObj('Router', ['navigate']);
        snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

        topicServiceSpy.getAll.and.returnValue(of([mockTopic]));

        await TestBed.configureTestingModule({
            imports: [CreatePostComponent],
            providers: [
                FormBuilder,
                { provide: PostService, useValue: postServiceSpy },
                { provide: TopicService, useValue: topicServiceSpy },
                { provide: ImageService, useValue: imageServiceSpy },
                { provide: Router, useValue: routerSpy },
                { provide: MatSnackBar, useValue: snackBarSpy },
                {
                    provide: ActivatedRoute,
                    useValue: { params: of({}) }
                }
            ]
        }).compileComponents();

        spyOn(console, 'error');
        fixture = TestBed.createComponent(CreatePostComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    // ---------- INIT ----------

    it('should initialize form and load topics', () => {
        component.ngOnInit();

        expect(component.createPostForm).toBeDefined();
        expect(topicServiceSpy.getAll).toHaveBeenCalled();
    });

    // ---------- TOPIC SEARCH ----------

    it('should filter topics on search', () => {
        component.allTopics = [
            { id: 1, name: 'Angular' },
            { id: 2, name: 'Java' }
        ] as Topic[];

        const event = { target: { value: 'ang' } } as any;

        component.onTopicSearchChange(event);

        expect(component.filteredTopics.length).toBe(1);
    });

    it('should show dropdown on focus', () => {
        component.allTopics = [mockTopic];

        component.onTopicFocus();

        expect(component.showTopicDropdown).toBeTrue();
    });

    it('should hide dropdown on blur', (done) => {
        component.showTopicDropdown = true;

        component.onTopicBlur();

        setTimeout(() => {
            expect(component.showTopicDropdown).toBeFalse();
            done();
        }, 200);
    });

    it('should select topic', () => {
        component.selectTopic(mockTopic);

        expect(component.selectedTopic).toEqual(mockTopic);
        expect(component.showTopicDropdown).toBeFalse();
    });

    it('should clear selected topic', () => {
        component.selectedTopic = mockTopic;

        component.clearSelectedTopic();

        expect(component.selectedTopic).toBeNull();
    });

    // ---------- DRAG & DROP ----------

    it('should set drag state on drag over', () => {
        const event = new DragEvent('dragover');

        component.onDragOver(event);

        expect(component.isDragOver).toBeTrue();
    });

    it('should remove drag state on drag leave', () => {
        const event = new DragEvent('dragleave');

        component.onDragLeave(event);

        expect(component.isDragOver).toBeFalse();
    });

    it('should call handleFiles on drop', () => {
        const file = new File(['test'], 'test.png', { type: 'image/png' });

        const event = {
            preventDefault() { },
            stopPropagation() { },
            dataTransfer: { files: [file] }
        } as any;

        spyOn<any>(component, 'handleFiles');

        component.onDrop(event);

        expect(component['handleFiles']).toHaveBeenCalled();
    });

    // ---------- IMAGE HANDLING ----------

    it('should add valid image files', () => {
        const file = new File(['test'], 'img.png', { type: 'image/png' });

        component['handleFiles']([file]);

        expect(component.selectedImages.length).toBe(1);
    });

    it('should remove image from preview list', () => {
        component.previewImages = ['img1', 'img2'];

        component.removeImage(0);

        expect(component.previewImages.length).toBe(1);
    });

    it('should not allow more than 10 images', () => {
        component.previewImages = new Array(10).fill(
            new File(['a'], 'a.png', { type: 'image/png' })
        );

        const newFile = new File(['test'], 'img.png', { type: 'image/png' });

        component['handleFiles']([newFile]);

        expect(component.errorMessage).toContain('10 images');
    });

    // ---------- GETTERS ----------

    it('should return title length', () => {
        component.createPostForm.patchValue({ title: 'Test' });

        expect(component.titleLength).toBe(4);
    });

    it('should return content length', () => {
        component.createPostForm.patchValue({ content: 'Content' });

        expect(component.contentLength).toBe(7);
    });

    // ---------- ERROR HANDLING ----------

    it('should set error message', () => {
        component.setError('Test error');

        expect(component.errorMessage).toBe('Test error');
    });

    // ---------- CREATE POST ----------

    it('should show error if form invalid', async () => {
        component.createPostForm.patchValue({ title: '' });

        await component.createPost();

        expect(component.errorMessage).toBeTruthy();
    });

    it('should create post successfully', async () => {
        postServiceSpy.create.and.returnValue(of(mockPost));

        component.selectedTopic = mockTopic;

        component.createPostForm.patchValue({
            title: 'Test Post',
            content: 'Content'
        });

        await component.createPost();

        expect(postServiceSpy.create).toHaveBeenCalled();
        expect(routerSpy.navigate).toHaveBeenCalled();
        expect(snackBarSpy.open).toHaveBeenCalled();
    });

    // ---------- IMAGE UPLOAD ----------

    it('should upload images before creating post', async () => {
        imageServiceSpy.uploadImages.and.returnValue(of({ urls: ['img1'] }) as any);
        postServiceSpy.create.and.returnValue(of(mockPost));

        component.selectedTopic = mockTopic;

        component.selectedImages = [
            new File(['test'], 'img.png', { type: 'image/png' })
        ];

        component.createPostForm.patchValue({
            title: 'Post',
            content: 'Content'
        });

        await component.createPost();

        expect(imageServiceSpy.uploadImages).toHaveBeenCalled();
    });

    it('should handle upload error', async () => {
        imageServiceSpy.uploadImages.and.returnValue(
            throwError(() => new Error('Upload error'))
        );

        component.selectedTopic = mockTopic;

        component.selectedImages = [
            new File(['test'], 'img.png', { type: 'image/png' })
        ];

        component.createPostForm.patchValue({
            title: 'Post',
            content: 'Content'
        });

        await component.createPost();

        expect(component.errorMessage).toBeTruthy();
    });

    // ---------- EDIT MODE ----------

    it('should load post for edit', () => {
        postServiceSpy.getById.and.returnValue(of(mockPost));

        component.postId = 1;

        component['loadPostForEdit']();

        expect(postServiceSpy.getById).toHaveBeenCalledWith(1);
    });

    it('should populate form with post', () => {
        component['populateFormWithPost'](mockPost);

        expect(component.createPostForm.value.title).toBe('Test Post');
        expect(component.selectedTopic).toEqual(mockTopic);
    });

    it('should update post when in edit mode', async () => {
        postServiceSpy.update.and.returnValue(of(mockPost));

        component.isEditMode = true;
        component.postId = 1;
        component.existingPost = mockPost;
        component.selectedTopic = mockTopic;

        component.createPostForm.patchValue({
            title: 'Updated title',
            content: 'Updated content'
        });

        await component.createPost();

        expect(postServiceSpy.update).toHaveBeenCalled();
        expect(snackBarSpy.open).toHaveBeenCalledWith(
            'Post updated successfully',
            'Close',
            jasmine.any(Object)
        );
        expect(routerSpy.navigate).toHaveBeenCalledWith(['/posts', 1]);
    });

    // ---------- CANCEL ----------

    it('should navigate to posts on cancel', () => {
        component.onCancel();

        expect(routerSpy.navigate).toHaveBeenCalledWith(['/posts']);
    });

});