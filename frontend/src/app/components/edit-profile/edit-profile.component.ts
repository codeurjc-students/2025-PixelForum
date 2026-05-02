import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDialog } from '@angular/material/dialog';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { ErrorService } from '../../services/error.service';
import { User } from '../../models/user.model';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

@Component({
    selector: 'app-edit-profile',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatIconModule,
        MatTabsModule
    ],
    templateUrl: './edit-profile.component.html',
    styleUrls: ['./edit-profile.component.scss']
})
export class EditProfileComponent implements OnInit, OnDestroy {
    profileForm: FormGroup;
    passwordForm: FormGroup;

    user: User | null = null;
    userId: number | null = null;
    isLoadingProfile = false;
    isLoadingPassword = false;
    profileErrorMessage = '';
    passwordErrorMessage = '';
    hideOldPassword = true;
    hideNewPassword = true;
    hideConfirmPassword = true;

    private originalProfileData: { username: string; email: string; bio: string } | null = null;
    private destroy$ = new Subject<void>();

    constructor(
        private fb: FormBuilder,
        private userService: UserService,
        private authService: AuthService,
        private errorService: ErrorService,
        private snackBar: MatSnackBar,
        public router: Router,
        private route: ActivatedRoute,
        private dialog: MatDialog
    ) {
        this.profileForm = this.createProfileForm();
        this.passwordForm = this.createPasswordForm();
    }

    ngOnInit(): void {
        this.route.params
            .pipe(takeUntil(this.destroy$))
            .subscribe(params => {
                if (params['userId']) {
                    this.userId = parseInt(params['userId'], 10);
                    this.loadUserProfile(this.userId);
                } else {
                    this.errorService.setError(400, 'Bad Request');
                    this.router.navigate(['/error']);
                }
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }

    private createProfileForm(): FormGroup {
        return this.fb.group({
            username: ['', [Validators.required, Validators.minLength(3)]],
            email: ['', [Validators.required, Validators.email]],
            bio: ['', [Validators.maxLength(255)]]
        });
    }

    private createPasswordForm(): FormGroup {
        return this.fb.group({
            oldPassword: ['', Validators.required],
            newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(64)]],
            confirmPassword: ['', Validators.required]
        }, {
            validators: this.passwordMatchValidator
        });
    }

    private passwordMatchValidator(group: FormGroup): null {
        const password = group.get('newPassword')?.value;
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

    private loadUserProfile(userId: number): void {
        this.userService.getById(userId)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: (user: User) => {
                    this.user = user;
                    this.originalProfileData = {
                        username: user.username,
                        email: user.email || '',
                        bio: user.bio || ''
                    };
                    this.profileForm.patchValue({
                        username: user.username,
                        email: user.email,
                        bio: user.bio || ''
                    });
                }
            });
    }

    onSubmitProfile(): void {
        if (!this.profileForm.valid || !this.userId) return;

        if (this.hasCriticalProfileChanges()) {
            this.openConfirmDialog(
                'Update Profile',
                'You are modifying critical account information. Please confirm you want to apply these changes.',
                () => this.updateProfile()
            );
        } else {
            this.updateProfile();
        }
    }

    private hasProfileChanges(): boolean {
        if (!this.originalProfileData) return false;
        const current = this.profileForm.value;
        return (
            current.username !== this.originalProfileData.username ||
            current.email !== this.originalProfileData.email ||
            current.bio !== this.originalProfileData.bio
        );
    }

    private hasCriticalProfileChanges(): boolean {
        if (!this.originalProfileData) return false;
        const current = this.profileForm.value;
        return (
            current.username !== this.originalProfileData.username ||
            current.email !== this.originalProfileData.email
        );
    }

    private updateProfile(): void {
        if (!this.userId) return;

        this.isLoadingProfile = true;
        this.profileErrorMessage = '';

        const { username, email, bio } = this.profileForm.value;

        this.userService.updateProfile(this.userId, { username, email, bio })
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: (user: User) => {
                    this.isLoadingProfile = false;
                    this.user = user;
                    this.originalProfileData = { username: user.username, email: user.email || '', bio: user.bio || '' };
                    this.authService.checkAuth().subscribe();
                    this.snackBar.open('Profile updated successfully', 'Close', { duration: 3000 });
                    this.router.navigate(['/users', this.userId]);
                },
                error: (err) => {
                    this.isLoadingProfile = false;
                    let errorMessage = 'Failed to update profile';

                    if (err.status === 400) {
                        if (err.error?.error?.includes('username')) {
                            errorMessage = 'Username already taken';
                        } else if (err.error?.error?.includes('email')) {
                            errorMessage = 'Email already in use';
                        } else {
                            errorMessage = err.error?.error || errorMessage;
                        }
                    } else if (err.status === 0) {
                        errorMessage = 'Unable to connect to the server';
                    }

                    this.profileErrorMessage = errorMessage;
                }
            });
    }

    onSubmitPassword(): void {
        if (!this.passwordForm.valid || !this.userId) return;

        this.openConfirmDialog(
            'Change Password',
            'Are you sure you want to change your password?',
            () => this.updatePassword()
        );
    }

    private updatePassword(): void {
        if (!this.userId) return;

        this.isLoadingPassword = true;
        this.passwordErrorMessage = '';

        const { oldPassword, newPassword } = this.passwordForm.value;

        this.userService.changePassword(this.userId, { oldPassword, newPassword })
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: () => {
                    this.isLoadingPassword = false;
                    this.passwordForm.reset();
                    this.snackBar.open('Password changed successfully', 'Close', { duration: 3000 });
                    this.router.navigate(['/users', this.userId]);
                },
                error: (err) => {
                    this.isLoadingPassword = false;
                    let errorMessage = 'Failed to change password';

                    if (err.status === 400) {
                        if (err.error?.error?.includes('old password') || err.error?.error?.includes('current password')) {
                            errorMessage = 'Current password is incorrect';
                        } else {
                            errorMessage = err.error?.error || errorMessage;
                        }
                    } else if (err.status === 0) {
                        errorMessage = 'Unable to connect to the server';
                    }

                    this.passwordErrorMessage = errorMessage;
                }
            });
    }

    private openConfirmDialog(title: string, message: string, onConfirm: () => void): void {
        const dialogRef = this.dialog.open(ConfirmDialogComponent, {
            width: '400px',
            autoFocus: false,
            data: {
                title,
                message,
                confirmText: 'Confirm',
                color: 'primary'
            }
        });

        dialogRef.afterClosed().subscribe(result => {
            if (result === true) {
                onConfirm();
            }
        });
    }

    toggleOldPasswordVisibility(): void {
        this.hideOldPassword = !this.hideOldPassword;
    }

    toggleNewPasswordVisibility(): void {
        this.hideNewPassword = !this.hideNewPassword;
    }

    toggleConfirmPasswordVisibility(): void {
        this.hideConfirmPassword = !this.hideConfirmPassword;
    }

    getProfileErrorMessage(fieldName: string): string {
        const field = this.profileForm.get(fieldName);
        if (!field || !field.errors) return '';

        if (fieldName === 'username') {
            if (field.hasError('required')) return 'Username is required';
            if (field.hasError('minlength')) return 'Username must be at least 3 characters';
        }

        if (fieldName === 'email') {
            if (field.hasError('required')) return 'Email is required';
            if (field.hasError('email')) return 'Please enter a valid email';
        }

        if (fieldName === 'bio') {
            if (field.hasError('maxlength')) return 'Bio must not exceed 255 characters';
        }

        return '';
    }

    getPasswordErrorMessage(fieldName: string): string {
        const field = this.passwordForm.get(fieldName);
        if (!field || !field.errors) return '';

        if (fieldName === 'oldPassword') {
            if (field.hasError('required')) return 'Current password is required';
        }

        if (fieldName === 'newPassword') {
            if (field.hasError('required')) return 'New password is required';
            if (field.hasError('minlength')) return 'Password must be at least 8 characters';
            if (field.hasError('maxlength')) return 'Password must not exceed 64 characters';
        }

        if (fieldName === 'confirmPassword') {
            if (field.hasError('required')) return 'Please confirm your password';
            if (field.hasError('passwordMismatch')) return 'Passwords do not match';
        }

        return '';
    }

    isProfileFieldInvalid(fieldName: string): boolean {
        const field = this.profileForm.get(fieldName);
        return !!(field && field.invalid && (field.dirty || field.touched));
    }

    isPasswordFieldInvalid(fieldName: string): boolean {
        const field = this.passwordForm.get(fieldName);
        return !!(field && field.invalid && (field.dirty || field.touched));
    }

    canSubmitProfile(): boolean {
        return this.profileForm.valid && this.hasProfileChanges() && !this.isLoadingProfile;
    }

    canSubmitPassword(): boolean {
        return this.passwordForm.valid && !this.isLoadingPassword;
    }
}