import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ErrorService {

	private error: { status: number; errorName: string; message: string } | null = null;

	setError(status: number, errorName: string, message: string): void {
		this.error = { status, errorName, message };
	}

	getError(): { status: number; errorName: string; message: string } | null {
		return this.error;
	}

	clear(): void {
		this.error = null;
	}
}