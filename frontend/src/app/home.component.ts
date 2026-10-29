import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from './services/auth.service';
import { ProfileService, ProfileResponse } from './services/profile.service';
import { ToastService } from './services/toast.service';

interface Slide {
  image: string;
  eyebrow: string;
  title: string;
  description: string;
}

interface ProfileField {
  label: string;
  value: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
})
export class HomeComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);

  currentSlide = 0;
  isProfileOpen = false;
  isProfileModalOpen = false;
  isProfileLoading = false;
  profileError = '';
  profileDetails: ProfileResponse | null = null;

  userName = 'User';
  userEmail = '';
  userInitial = 'U';

  isLoggingOut = false;
  logoutError = '';

  private carouselInterval?: ReturnType<typeof setInterval>;

  isPasswordResetOpen = false;
  isCurrentPasswordVerified = false;
  isVerifyingCurrentPassword = false;
  isResettingPassword = false;
  showCurrentPassword = false;
  showNewPassword = false;
  showConfirmPassword = false;
  currentPasswordError = '';
  resetPasswordError = '';
  resetPasswordSuccess = '';

  readonly passwordPattern =
    /^(?=.*[A-Z].*[A-Z])(?=.*[!@#$&*])(?=.*[0-9].*[0-9])(?=.*[a-z].*[a-z].*[a-z]).{8,}$/;

  currentPasswordForm = this.fb.group({
    currentPassword: ['', Validators.required],
  });

  resetPasswordForm = this.fb.group({
    password: ['', [Validators.required, Validators.pattern(this.passwordPattern)]],
    confirmPassword: ['', Validators.required],
  });

  slides: Slide[] = [
    {
      image:
        'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=2000&q=85',
      eyebrow: 'DISCOVER • EXPLORE • EXPERIENCE',
      title: 'Beautiful moments begin here.',
      description:
        'Explore inspiring places, beautiful experiences and everything that makes life memorable.',
    },
    {
      image:
        'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=2000&q=85',
      eyebrow: 'ESCAPE • DREAM • CREATE',
      title: 'Escape the ordinary.',
      description: 'Take a break from everyday life and discover something beautifully different.',
    },
    {
      image:
        'https://images.unsplash.com/photo-1470770841072-f978cf4d019e?auto=format&fit=crop&w=2000&q=85',
      eyebrow: 'LIVE • INSPIRE • GROW',
      title: 'Find your inspiration.',
      description: 'A thoughtfully crafted space designed to help you discover, explore and grow.',
    },
  ];

  constructor(
    private readonly router: Router,
    private readonly profileService: ProfileService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadUser();
    this.startCarousel();
  }

  ngOnDestroy(): void {
    this.stopCarousel();
  }

  get profileInitial(): string {
    const profile = this.profileDetails as
      (ProfileResponse & { name?: string; email?: string }) | null;

    const name = profile?.name || profile?.email || this.userName || 'U';
    return name.charAt(0).toUpperCase();
  }

  get profileFields(): ProfileField[] {
    if (!this.profileDetails) {
      return [];
    }

    return Object.entries(this.profileDetails as Record<string, unknown>)
      .filter(([, value]) => value !== null && value !== undefined && typeof value !== 'object')
      .map(([key, value]) => ({
        label: key
          .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
          .replace(/^./, (character) => character.toUpperCase()),
        value: String(value),
      }));
  }

  get newPasswordControl() {
    return this.resetPasswordForm.controls.password;
  }

  get confirmPasswordControl() {
    return this.resetPasswordForm.controls.confirmPassword;
  }

  get passwordsMatch(): boolean {
    return (
      !!this.confirmPasswordControl.value &&
      this.newPasswordControl.value === this.confirmPasswordControl.value
    );
  }

  loadUser(): void {
    const storedUser = localStorage.getItem('user');

    if (!storedUser) {
      return;
    }

    try {
      const user = JSON.parse(storedUser);
      this.userName = user.name || user.email || 'User';
      this.userEmail = user.email || '';
      this.userInitial = this.userName.charAt(0).toUpperCase();
    } catch {
      localStorage.removeItem('user');
      this.userName = 'User';
      this.userEmail = '';
      this.userInitial = 'U';
    }
  }

  viewProfile(): void {
    this.isProfileOpen = false;
    this.isProfileModalOpen = true;
    this.isProfileLoading = true;
    this.profileError = '';
    this.cdr.detectChanges();

    this.profileService.getUserDetails().subscribe({
      next: (response: ProfileResponse) => {
        this.profileDetails = response;
        this.isProfileLoading = false;

        const profile = response as Record<string, unknown>;
        this.userName = String(profile['name'] || profile['email'] || 'User');
        this.userEmail = String(profile['email'] || '');
        this.userInitial = this.userName.charAt(0).toUpperCase();

        this.cdr.detectChanges();
      },
      error: (error: HttpErrorResponse) => {
        console.error('Unable to load profile:', error);
        this.isProfileLoading = false;

        this.profileError =
          error.status === 401 || error.status === 403
            ? 'You are not authorized to view this profile. Please sign in again.'
            : 'Unable to load your profile. Please try again.';

        this.cdr.detectChanges();
      },
    });
  }

  closeProfile(): void {
    this.isProfileModalOpen = false;
    this.closePasswordReset();
  }

  startPasswordReset(): void {
    this.isPasswordResetOpen = true;
    this.isCurrentPasswordVerified = false;
    this.currentPasswordError = '';
    this.resetPasswordError = '';
    this.resetPasswordSuccess = '';
    this.currentPasswordForm.reset();
    this.resetPasswordForm.reset();
    this.showCurrentPassword = false;
    this.showNewPassword = false;
    this.showConfirmPassword = false;
  }

  closePasswordReset(): void {
    this.isPasswordResetOpen = false;
    this.isCurrentPasswordVerified = false;
    this.isVerifyingCurrentPassword = false;
    this.isResettingPassword = false;
    this.currentPasswordError = '';
    this.resetPasswordError = '';
    this.resetPasswordSuccess = '';
    this.currentPasswordForm.reset();
    this.resetPasswordForm.reset();
    this.showCurrentPassword = false;
    this.showNewPassword = false;
    this.showConfirmPassword = false;
  }

  verifyCurrentPassword(): void {
    this.currentPasswordError = '';
    this.resetPasswordError = '';
    this.resetPasswordSuccess = '';
    this.currentPasswordForm.markAllAsTouched();

    if (this.currentPasswordForm.invalid || this.isVerifyingCurrentPassword) {
      return;
    }

    const currentPassword = this.currentPasswordForm.controls.currentPassword.value;

    if (!currentPassword) {
      return;
    }

    this.isVerifyingCurrentPassword = true;

    this.profileService
      .verifyCurrentPassword(currentPassword)
      .pipe(finalize(() => (this.isVerifyingCurrentPassword = false)))
      .subscribe({
        next: () => {
          this.isCurrentPasswordVerified = true;
          this.currentPasswordError = '';
          this.resetPasswordForm.reset();
          this.showNewPassword = false;
          this.showConfirmPassword = false;
        },
        error: (error: HttpErrorResponse) => {
          this.currentPasswordError = this.getBackendErrorMessage(
            error,
            'Unable to verify your current password. Please try again.',
          );
        },
      });
  }

  hasPasswordRequirement(requirement: string): boolean {
    const password = this.newPasswordControl.value ?? '';

    switch (requirement) {
      case 'length':
        return password.length >= 8;
      case 'uppercase':
        return (password.match(/[A-Z]/g) ?? []).length >= 2;
      case 'special':
        return /[!@#$&*]/.test(password);
      case 'number':
        return (password.match(/[0-9]/g) ?? []).length >= 2;
      case 'lowercase':
        return (password.match(/[a-z]/g) ?? []).length >= 3;
      default:
        return false;
    }
  }

  resetPassword(): void {
    this.resetPasswordError = '';
    this.resetPasswordSuccess = '';
    this.resetPasswordForm.markAllAsTouched();

    if (!this.isCurrentPasswordVerified) {
      this.resetPasswordError = 'Verify your current password first.';
      return;
    }

    if (this.resetPasswordForm.invalid) {
      return;
    }

    if (!this.passwordsMatch) {
      this.resetPasswordError = 'The new passwords do not match.';
      return;
    }

    const password = this.newPasswordControl.value;

    if (!password || this.isResettingPassword) {
      return;
    }

    this.isResettingPassword = true;

    this.profileService
      .resetPassword(password)
      .pipe(finalize(() => (this.isResettingPassword = false)))
      .subscribe({
        next: () => {
          this.resetPasswordSuccess = 'Your password has been updated successfully.';

          this.currentPasswordForm.reset();
          this.resetPasswordForm.reset();
          this.isCurrentPasswordVerified = false;
          this.isPasswordResetOpen = false;
          this.showCurrentPassword = false;
          this.showNewPassword = false;
          this.showConfirmPassword = false;
        },
        error: (error: HttpErrorResponse) => {
          this.resetPasswordError = this.getBackendErrorMessage(
            error,
            'Unable to reset your password. Please try again.',
          );
        },
      });
  }

  logout(): void {
    if (this.isLoggingOut) {
      return;
    }

    this.isLoggingOut = true;
    this.logoutError = '';
    this.isProfileOpen = false;
    this.cdr.detectChanges();

    this.authService
      .logout()
      .pipe(
        finalize(() => {
          this.clearLocalSession();
          this.isLoggingOut = false;
        }),
      )
      .subscribe({
        next: () => {
          void this.router.navigateByUrl('/').then(() => {
            this.toastService.success(
              'Logged out successfully',
              'You have been securely logged out of your HelpDesk account.',
            );
          });
        },
        error: (error: HttpErrorResponse) => {
          console.error('Backend logout request failed:', error);

          // End the local session even if the API is unavailable.
          // The server-side cookie may remain if the request never reached it.
          this.logoutError =
            error.status === 0
              ? 'The server could not be reached. You have been signed out locally.'
              : 'Your local session has been cleared, but the server could not confirm logout.';

          const message = this.logoutError;

          void this.router.navigateByUrl('/').then(() => {
            this.toastService.warning('Logout could not be confirmed', message);
          });
        },
      });
  }

  private clearLocalSession(): void {
    localStorage.removeItem('isLoggedIn');
    localStorage.removeItem('user');

    this.userName = 'User';
    this.userEmail = '';
    this.userInitial = 'U';

    this.isProfileOpen = false;
    this.isProfileModalOpen = false;
    this.profileDetails = null;
    this.profileError = '';
    this.profileError = '';
    this.closePasswordReset();
  }

  private getBackendErrorMessage(error: HttpErrorResponse, fallback: string): string {
    const body = error.error;

    if (typeof body === 'string' && body.trim()) {
      return body;
    }

    if (body && typeof body === 'object') {
      return body.message || body.detail || body.defaultMessage || body.title || fallback;
    }

    return fallback;
  }

  startCarousel(): void {
    this.stopCarousel();

    this.carouselInterval = setInterval(() => {
      this.nextSlide();
      this.cdr.detectChanges();
    }, 5000);
  }

  stopCarousel(): void {
    if (this.carouselInterval) {
      clearInterval(this.carouselInterval);
      this.carouselInterval = undefined;
    }
  }

  nextSlide(): void {
    this.currentSlide = (this.currentSlide + 1) % this.slides.length;
  }

  previousSlide(): void {
    this.currentSlide = (this.currentSlide - 1 + this.slides.length) % this.slides.length;
  }

  selectSlide(index: number): void {
    this.currentSlide = index;
    this.startCarousel();
    this.cdr.detectChanges();
  }

  toggleProfile(): void {
    this.isProfileOpen = !this.isProfileOpen;
    this.cdr.detectChanges();
  }
}
