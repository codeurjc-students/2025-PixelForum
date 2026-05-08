import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatCardModule } from '@angular/material/card';
import { UserService } from '../../services/user.service';

@Component({
    selector: 'app-register',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatIconModule,
        MatCardModule,
        RouterLink
    ],
    templateUrl: './register.component.html',
    styleUrls: ['./register.component.scss']
})
export class RegisterComponent {
    registerForm: FormGroup;
    isLoading = false;
    hidePassword = true;
    hideConfirmPassword = true;
    errorMessage = '';

    constructor(
        private fb: FormBuilder,
        private userService: UserService,
        private snackBar: MatSnackBar,
        private router: Router
    ) {
        this.registerForm = this.createForm();
    }

    private createForm(): FormGroup {
        return this.fb.group({
            username: ['', [Validators.required, Validators.minLength(3)]],
            email: ['', [Validators.required, Validators.email]],
            password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(64)]],
            confirmPassword: ['', Validators.required],
            bio: ['', [Validators.maxLength(255)]]
        }, { validators: this.passwordMatchValidator });
    }

    private passwordMatchValidator(group: FormGroup): null {
        const password = group.get('password')?.value;
        const confirmControl = group.get('confirmPassword');

        if (!confirmControl) return null;

        if (!password || !confirmControl.value) {
            return null;
        }

        if (password !== confirmControl.value) {
            confirmControl.setErrors({ passwordMismatch: true });
        } else {
            if (confirmControl.hasError('passwordMismatch')) {
                const errors = { ...confirmControl.errors };
                delete errors['passwordMismatch'];
                confirmControl.setErrors(Object.keys(errors).length ? errors : null);
            }
        }

        return null;
    }

    togglePasswordVisibility(): void {
        this.hidePassword = !this.hidePassword;
    }

    toggleConfirmPasswordVisibility(): void {
        this.hideConfirmPassword = !this.hideConfirmPassword;
    }

    onSubmit(): void {
        if (!this.registerForm.valid) return;

        this.isLoading = true;
        this.errorMessage = '';
        const { username, email, password, bio } = this.registerForm.value;

        const registrationData = {
            username,
            email,
            password,
            bio: bio?.trim() ? bio : null
        };

        this.userService.register(registrationData).subscribe({
            next: () => {
                this.isLoading = false;
                this.snackBar.open('Account created successfully!', 'Close', {
                    duration: 3000
                });
                this.router.navigate(['/login']);
            },
            error: err => {
                this.isLoading = false;
                let errorMessage = 'An error occurred. Please try again.';

                if (err.status === 400) {
                    errorMessage = err.error?.message || errorMessage;
                } else if (err.status === 0) {
                    errorMessage = 'Unable to connect to the server';
                }

                this.errorMessage = errorMessage;
            }
        });
    }

    getErrorMessage(fieldName: string): string {
        const field = this.registerForm.get(fieldName);

        if (!field || !field.errors) return '';

        if (fieldName === 'username') {
            if (field.hasError('required')) return 'Please enter your username';
            if (field.hasError('minlength')) return 'Username must be at least 3 characters';
        }

        if (fieldName === 'email') {
            if (field.hasError('required')) return 'Please enter your email';
            if (field.hasError('email')) return 'Please enter a valid email';
        }

        if (fieldName === 'password') {
            if (field.hasError('required')) return 'Please enter your password';
            if (field.hasError('minlength')) return 'Password must be at least 8 characters';
            if (field.hasError('maxlength')) return 'Password must not exceed 64 characters';
        }

        if (fieldName === 'confirmPassword') {
            if (field.hasError('required')) return 'Please confirm your password';
            if (field.hasError('passwordMismatch')) return 'Passwords do not match';
        }

        if (fieldName === 'bio') {
            if (field.hasError('maxlength')) return 'Bio must not exceed 255 characters';
        }

        return '';
    }

    isFieldInvalid(fieldName: string): boolean {
        const field = this.registerForm.get(fieldName);
        return !!(field && field.invalid && (field.dirty || field.touched));
    }
}