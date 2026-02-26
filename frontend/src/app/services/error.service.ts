import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ErrorService {

	private error: { status: number; errorName: string } | null = null;

	setError(status: number, errorName: string): void {
		this.error = { status, errorName };
	}

	getError(): { status: number; errorName: string } | null {
		return this.error;
	}

	clear(): void {
		this.error = null;
	}
}