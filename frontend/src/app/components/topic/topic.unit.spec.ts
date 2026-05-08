import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { TopicComponent } from './topic.component';
import { TopicService } from '../../services/topic.service';
import { ActivatedRoute, Router } from '@angular/router';
import { Topic } from '../../models/topic.model';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('TopicComponent - Unit Tests', () => {

    let component: TopicComponent;
    let fixture: ComponentFixture<TopicComponent>;
    let topicService: jasmine.SpyObj<TopicService>;
    let router: jasmine.SpyObj<Router>;

    const mockTopic: Topic = {
        id: 1,
        name: 'GTA VI'
    } as any;

    let routeParams$: Subject<any>;

    beforeEach(async () => {
        spyOn(console, 'error');

        topicService = jasmine.createSpyObj('TopicService', ['getById']);
        router = jasmine.createSpyObj('Router', ['navigate']);

        // Subject to control observables in tests
        routeParams$ = new Subject();

        const activatedRouteMock = {
            params: routeParams$.asObservable()
        };

        await TestBed.configureTestingModule({
            imports: [TopicComponent, HttpClientTestingModule],
            providers: [
                { provide: TopicService, useValue: topicService },
                { provide: Router, useValue: router },
                { provide: ActivatedRoute, useValue: activatedRouteMock }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(TopicComponent);
        component = fixture.componentInstance;
    });

    // ---------- INIT ----------

    it('should create component', () => {
        expect(component).toBeTruthy();
    });

    it('should load topic on init when topicId exists', () => {
        spyOn(component, 'loadTopic');

        component.ngOnInit();

        routeParams$.next({ topicId: 1 });

        expect(component.loadTopic).toHaveBeenCalledWith(1);
    });

    // ---------- LOAD TOPIC ----------

    it('should load topic and update state correctly', () => {
        topicService.getById.and.returnValue(of(mockTopic));

        component.loadTopic(1);

        expect(topicService.getById).toHaveBeenCalledWith(1);
        expect(component.topic).toEqual(mockTopic);
        expect(component.filterTopicName).toBe('GTA VI');
        expect(component.isLoadingTopic).toBeFalse();
    });

    // ---------- NAVIGATION ----------

    it('should go back if history length > 1', () => {
        spyOn(window.history, 'back');
        spyOnProperty(window.history, 'length').and.returnValue(2);

        component.goBack();

        expect(window.history.back).toHaveBeenCalled();
    });

    it('should navigate to posts if no history', () => {
        spyOnProperty(window.history, 'length').and.returnValue(1);

        component.goBack();

        expect(router.navigate).toHaveBeenCalledWith(['/posts']);
    });

    // ---------- DESTROY ----------

    it('should complete destroy$ on ngOnDestroy', () => {
        spyOn(component['destroy$'], 'next');
        spyOn(component['destroy$'], 'complete');

        component.ngOnDestroy();

        expect(component['destroy$'].next).toHaveBeenCalled();
        expect(component['destroy$'].complete).toHaveBeenCalled();
    });
});