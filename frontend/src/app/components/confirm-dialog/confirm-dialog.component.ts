import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';

export interface ConfirmDialogData {
	title: string;
	message: string;
	detail?: string;
	confirmText?: string;
	color?: string;
	showSecondaryAction?: boolean;
	secondaryActionText?: string;
	secondaryActionColor?: string;
	icon?: string;
}

@Component({
	selector: 'app-confirm-dialog',
	standalone: true,
	imports: [CommonModule, MatDialogModule, MatButtonModule, MatProgressSpinnerModule, MatIconModule],
	templateUrl: './confirm-dialog.component.html',
	styleUrls: ['./confirm-dialog.component.scss']
})
export class ConfirmDialogComponent {

	constructor(
		public dialogRef: MatDialogRef<ConfirmDialogComponent>,
		@Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData
	) { }

	onCancel(): void {
		this.dialogRef.close(false);
	}

	onConfirm(): void {
		this.dialogRef.close(true);
	}

	onSecondaryAction(): void {
		this.dialogRef.close('secondary');
	}
}