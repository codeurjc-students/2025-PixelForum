import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject, of } from 'rxjs';
import { HeaderComponent } from './header.component';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { RouterTestingModule } from '@angular/router/testing';
import { TopicService } from '../../services/topic.service';
import { Router } from '@angular/router';
import { Topic } from '../../models/topic.model';

describe('HeaderComponent - Unit Tests', () => {
	let component: HeaderComponent;
	let fixture: ComponentFixture<HeaderComponent>;
	let authService: jasmine.SpyObj<AuthService>;
	let loggedInSubject: BehaviorSubject<boolean>;
	let topicService: jasmine.SpyObj<TopicService>;
	let router: Router;

	const topics = [
		{ id: 1, name: 'Angular' },
		{ id: 2, name: 'Spring Boot' },
		{ id: 3, name: 'Angular Material' }
	] as Topic[];

	beforeEach(async () => {
		loggedInSubject = new BehaviorSubject<boolean>(true);

		// Mock TopicService
		topicService = jasmine.createSpyObj('TopicService', ['getAll']);
		topicService.getAll.and.returnValue(of(topics));

		// Mock AuthService
		authService = jasmine.createSpyObj('AuthService', ['checkAuth', 'logout'], {
			loggedIn$: loggedInSubject.asObservable()
		});

		authService.checkAuth.and.returnValue(of(null));
		authService.logout.and.returnValue(of({}));
		authService.user$ = of({ id: 1, username: 'testuser', roles: ['USER'] });

		await TestBed.configureTestingModule({
			imports: [
				HeaderComponent,
				RouterTestingModule,
				CommonModule
			],
			providers: [
				{ provide: AuthService, useValue: authService },
				{ provide: TopicService, useValue: topicService }
			]
		}).compileComponents();

		fixture = TestBed.createComponent(HeaderComponent);
		component = fixture.componentInstance;
		router = TestBed.inject(Router);
		spyOn(router, 'navigate');
		fixture.detectChanges();
	});

	it('should create component', () => {
		expect(component).toBeTruthy();
	});

	it('should call authService.checkAuth on init', () => {
		component.ngOnInit();
		expect(authService.checkAuth).toHaveBeenCalled();
	});

	it('should call authService.logout when onLogout is called', () => {
		component.onLogout();
		expect(authService.logout).toHaveBeenCalled();
	});

	it('should render logout button when logged in', () => {
		fixture.detectChanges();
		const compiled = fixture.nativeElement as HTMLElement;
		const logoutBtn = compiled.querySelector('#logout-button');
		expect(logoutBtn).toBeTruthy();
	});

	it('should not render logout button when not logged in', () => {
		loggedInSubject.next(false);
		fixture.detectChanges();

		const compiled = fixture.nativeElement as HTMLElement;
		const logoutBtn = compiled.querySelector('#logout-button');
		expect(logoutBtn).toBeNull();
	});

	it('should load topics on init', () => {
		expect(topicService.getAll).toHaveBeenCalled();
		expect(component.topics).toEqual(topics);
	});

	it('should filter topics by name ignoring case', () => {
		component.onSearch('ANGULAR');
		expect(component.results.map(t => t.name)).toEqual(['Angular', 'Angular Material']);
	});

	it('should clear results when query is empty or blank', () => {
		component.onSearch('angular');
		component.onSearch('   ');
		expect(component.results).toEqual([]);
	});

	it('should limit results to 8', () => {
		component.topics = Array.from({ length: 10 }, (_, i) => ({ id: i, name: `Topic ${i}` }) as Topic);
		component.onSearch('topic');
		expect(component.results.length).toBe(8);
	});

	it('should navigate to the topic and reset the search on goToTopic', () => {
		const input = document.createElement('input');
		input.value = 'angular';
		component.results = [topics[0]];

		component.goToTopic(topics[0], input);

		expect(router.navigate).toHaveBeenCalledWith(['/topics', 1]);
		expect(component.results).toEqual([]);
		expect(input.value).toBe('');
	});

	it('should render results in the dropdown when typing', () => {
		const compiled = fixture.nativeElement as HTMLElement;
		const input = compiled.querySelector('.search input') as HTMLInputElement;

		input.value = 'spring';
		input.dispatchEvent(new Event('input'));
		fixture.detectChanges();

		const items = compiled.querySelectorAll('.search-results li');
		expect(items.length).toBe(1);
		expect(items[0].textContent).toContain('Spring Boot');
	});

	it('should navigate to the first result on Enter', () => {
		const compiled = fixture.nativeElement as HTMLElement;
		const input = compiled.querySelector('.search input') as HTMLInputElement;

		input.value = 'angular';
		input.dispatchEvent(new Event('input'));
		input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));

		expect(router.navigate).toHaveBeenCalledWith(['/topics', 1]);
	});

	it('should not navigate on Enter when there are no results', () => {
		const compiled = fixture.nativeElement as HTMLElement;
		const input = compiled.querySelector('.search input') as HTMLInputElement;

		input.value = 'zzz';
		input.dispatchEvent(new Event('input'));
		input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));

		expect(router.navigate).not.toHaveBeenCalled();
	});

	it('should navigate when clicking a result (mousedown)', () => {
		const compiled = fixture.nativeElement as HTMLElement;
		const input = compiled.querySelector('.search input') as HTMLInputElement;

		input.value = 'spring';
		input.dispatchEvent(new Event('input'));
		fixture.detectChanges();

		const item = compiled.querySelector('.search-results li') as HTMLElement;
		item.dispatchEvent(new Event('mousedown'));

		expect(router.navigate).toHaveBeenCalledWith(['/topics', 2]);
	});

	it('should hide results on blur', () => {
		const compiled = fixture.nativeElement as HTMLElement;
		const input = compiled.querySelector('.search input') as HTMLInputElement;

		input.value = 'angular';
		input.dispatchEvent(new Event('input'));
		input.dispatchEvent(new Event('blur'));
		fixture.detectChanges();

		expect(compiled.querySelector('.search-results')).toBeNull();
	});
});