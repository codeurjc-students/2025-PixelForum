import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PostDetailComponent } from './post-detail.component';
import { PostService } from '../../services/post.service';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';
import { of } from 'rxjs';
import { Post } from '../../models/post.model';
import { PostComponent } from '../post/post.component';

describe('PostDetailComponent', () => {

    let component: PostDetailComponent;
    let fixture: ComponentFixture<PostDetailComponent>;

    let postServiceSpy: jasmine.SpyObj<PostService>;
    let routerSpy: jasmine.SpyObj<Router>;
    let locationSpy: jasmine.SpyObj<Location>;

    const mockPost: Post = {
        id: 1,
        title: 'Test Post',
        content: 'Test Content',
        topic: { id: 1, name: 'Angular' } as any,
        images: []
    };

    beforeEach(async () => {
        postServiceSpy = jasmine.createSpyObj('PostService', ['getById']);
        routerSpy = jasmine.createSpyObj('Router', ['navigate']);
        locationSpy = jasmine.createSpyObj('Location', ['back']);

        await TestBed.configureTestingModule({
            imports: [PostDetailComponent, PostComponent],
            providers: [
                { provide: PostService, useValue: postServiceSpy },
                { provide: Router, useValue: routerSpy },
                { provide: Location, useValue: locationSpy },
                {
                    provide: ActivatedRoute,
                    useValue: { params: of({ id: 1 }) }
                }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(PostDetailComponent);
        component = fixture.componentInstance;
        spyOn(console, 'error');
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    // ---------- INIT ----------

    it('should load post if id param exists', () => {
        postServiceSpy.getById.and.returnValue(of(mockPost));

        component.ngOnInit();

        expect(postServiceSpy.getById).toHaveBeenCalledWith(1);
        expect(component.post).toEqual(mockPost);
        expect(component.loading).toBeFalse();
    });

    it('should not load post if no id param', () => {
        const route = TestBed.inject(ActivatedRoute) as any;
        route.params = of({});

        component.ngOnInit();

        expect(component.post).toBeNull();
        expect(component.loading).toBeFalse();
    });

    // ---------- LOAD POST ----------

    it('should set loading while fetching', () => {
        postServiceSpy.getById.and.returnValue(of(mockPost));

        component.loadPost(1);

        expect(component.loading).toBeFalse();
        expect(component.post).toEqual(mockPost);
    });

    // ---------- GO BACK ----------

    it('should call location.back if history length > 1', () => {
        spyOnProperty(window.history, 'length', 'get').and.returnValue(2);

        component.goBack();

        expect(locationSpy.back).toHaveBeenCalled();
        expect(routerSpy.navigate).not.toHaveBeenCalled();
    });

    it('should navigate to /posts if history length <= 1', () => {
        spyOnProperty(window.history, 'length', 'get').and.returnValue(1);

        component.goBack();

        expect(locationSpy.back).not.toHaveBeenCalled();
        expect(routerSpy.navigate).toHaveBeenCalledWith(['/posts']);
    });

});