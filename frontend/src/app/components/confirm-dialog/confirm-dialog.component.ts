import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

export interface ConfirmDialogData {
    title: string;
    message: string;
    detail?: string;
    confirmText?: string;
    cancelText?: string;
    color?: 'primary' | 'accent' | 'warn';
}

@Component({
    selector: 'app-confirm-dialog',
    standalone: true,
    imports: [CommonModule, MatDialogModule, MatButtonModule, MatProgressSpinnerModule],
    templateUrl: './confirm-dialog.component.html',
    styleUrls: ['./confirm-dialog.component.scss']
})
export class ConfirmDialogComponent {
    isLoading = false;

    constructor(
        public dialogRef: MatDialogRef<ConfirmDialogComponent>,
        @Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData
    ) { }

    onCancel(): void {
        this.dialogRef.close(false);
    }

    onConfirm(): void {
        this.isLoading = true;
        this.dialogRef.close(true);
    }
}