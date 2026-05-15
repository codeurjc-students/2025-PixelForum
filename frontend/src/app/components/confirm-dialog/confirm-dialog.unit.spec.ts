import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConfirmDialogComponent, ConfirmDialogData } from './confirm-dialog.component';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

describe('ConfirmDialogComponent', () => {
    let component: ConfirmDialogComponent;
    let fixture: ComponentFixture<ConfirmDialogComponent>;
    let dialogSpy: jasmine.SpyObj<MatDialogRef<ConfirmDialogComponent>>;

    const mockData: ConfirmDialogData = {
        title: 'Delete item',
        message: 'Are you sure?',
        confirmText: 'Yes'
    };

    beforeEach(async () => {
        dialogSpy = jasmine.createSpyObj('MatDialogRef', ['close']);

        await TestBed.configureTestingModule({
            imports: [ConfirmDialogComponent],
            providers: [
                { provide: MAT_DIALOG_DATA, useValue: mockData },
                { provide: MatDialogRef, useValue: dialogSpy }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(ConfirmDialogComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should close dialog with false when cancel is clicked', () => {
        component.onCancel();
        expect(dialogSpy.close).toHaveBeenCalledWith(false);
    });

    it('should set loading and close dialog with true when confirm is clicked', () => {
        component.onConfirm();
        expect(dialogSpy.close).toHaveBeenCalledWith(true);
    });

    it('should close dialog with secondary when secondary action is clicked', () => {
        component.onSecondaryAction();
        expect(dialogSpy.close).toHaveBeenCalledWith('secondary');
    });
});