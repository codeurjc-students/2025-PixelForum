import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ErrorComponent } from './error.component';
import { Router } from '@angular/router';
import { ErrorService } from '../../services/error.service';

describe('ErrorComponent', () => {

    let component: ErrorComponent;
    let fixture: ComponentFixture<ErrorComponent>;

    let errorServiceSpy: jasmine.SpyObj<ErrorService>;
    let routerSpy: jasmine.SpyObj<Router>;

    const mockError = { status: 500, errorName: 'Internal Server Error' };

    beforeEach(async () => {
        errorServiceSpy = jasmine.createSpyObj('ErrorService', ['getError', 'clear']);
        routerSpy = jasmine.createSpyObj('Router', ['navigate']);

        await TestBed.configureTestingModule({
            imports: [ErrorComponent],
            providers: [
                { provide: ErrorService, useValue: errorServiceSpy },
                { provide: Router, useValue: routerSpy }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(ErrorComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    // ---------- INIT ----------

    it('should set error from service on init', () => {
        errorServiceSpy.getError.and.returnValue(mockError);

        component.ngOnInit();

        expect(component.status).toBe(500);
        expect(component.errorName).toBe('Internal Server Error');
    });

    it('should default to 404 if no error', () => {
        errorServiceSpy.getError.and.returnValue(null);

        component.ngOnInit();

        expect(component.status).toBe(404);
        expect(component.errorName).toBe('Page not found');
    });

    // ---------- GO HOME ----------

    it('should clear error and navigate home', () => {
        component.goHome();

        expect(errorServiceSpy.clear).toHaveBeenCalled();
        expect(routerSpy.navigate).toHaveBeenCalledWith(['posts']);
    });

});