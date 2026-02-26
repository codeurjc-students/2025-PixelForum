import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ErrorService } from '../../services/error.service';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';

@Component({
	selector: 'app-error',
	standalone: true,
	imports: [CommonModule, MatCardModule, MatButtonModule],
	templateUrl: './error.component.html',
	styleUrls: ['./error.component.scss']
})
export class ErrorComponent implements OnInit {

	status!: number;
	errorName!: string;

	constructor(
		private errorService: ErrorService,
		private router: Router
	) { }

	ngOnInit(): void {
		const error = this.errorService.getError();

		if (!error) {
			this.status = 404;
			this.errorName = 'Page not found';
			return;
		}

		this.status = error.status;
		this.errorName = error.errorName;
	}

	goHome(): void {
		this.errorService.clear();
		this.router.navigate(['posts']);
	}
}