import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnDestroy, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, finalize } from 'rxjs';
import { AuthService } from './services/auth.service';
import { ProfileService } from './services/profile.service';
import { ToastComponent } from './components/toast/toast.component';
import { resetSessionExpirationState } from './services/auth.interceptor';
import { ToastService } from './services/toast.service';

type AuthScreen = 'login' | 'register' | 'email' | 'otp' | 'reset' | 'complete';
const PASSWORD_PATTERN = /^(?=.*[A-Z].*[A-Z])(?=.*[!@#$&*])(?=.*[0-9].*[0-9])(?=.*[a-z].*[a-z].*[a-z]).{8,}$/;

function matchingPasswords(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password')?.value;
  const confirm = control.get('confirmPassword')?.value;
  return password && confirm && password !== confirm ? { passwordMismatch: true } : null;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterOutlet, ToastComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})

export class App implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly profileService = inject(ProfileService);
  private readonly API_URL = 'http://localhost:8080/auth';
  private countdownInterval?: ReturnType<typeof setInterval>;
  private redirectTimeout?: ReturnType<typeof setTimeout>;
  private otpExpiresAt = 0;
  private lastRouteUrl = this.router.url;
  private readonly toastService = inject(ToastService);

  screen: AuthScreen = 'login';
  isLoading = false;
  isLoggingOut = false;
  errorMessage = '';
  successMessage = '';
  showPassword = false;
  showConfirmPassword = false;
  otpDigits: string[] = ['', '', '', '', '', ''];
  remainingSeconds = 300;
  verifiedEmail = '';
  completionMessage = '';

  loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });
  registerForm = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
  });
  emailForm = this.fb.group({ email: ['', [Validators.required, Validators.email]] });
  resetForm = this.fb.group(
    {
      password: ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
      confirmPassword: ['', Validators.required],
    },
    { validators: matchingPasswords },
  );

  constructor() {
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event) => {
        const nextUrl = event.urlAfterRedirects;
        // Do not reset an in-progress OTP/password recovery flow just because
        // Angular emits a navigation event for the already-active root route.
        // Only reset when actually returning from the dashboard to sign in.
        if ((nextUrl === '/' || nextUrl === '') && this.lastRouteUrl.startsWith('/home')) {
          this.resetAuthScreen();
        }
        this.lastRouteUrl = nextUrl;
      });
  }

  get isHomePage(): boolean {
    return this.router.url.startsWith('/home');
  }
  get loginEmail() {
    return this.loginForm.controls.email;
  }
  get loginPassword() {
    return this.loginForm.controls.password;
  }
  get registerName() {
    return this.registerForm.controls.name;
  }
  get registerEmail() {
    return this.registerForm.controls.email;
  }
  get registerPassword() {
    return this.registerForm.controls.password;
  }
  get otpValue(): string {
    return this.otpDigits.join('');
  }
  get formattedTime(): string {
    return `${Math.floor(this.remainingSeconds / 60)
      .toString()
      .padStart(2, '0')}:${(this.remainingSeconds % 60).toString().padStart(2, '0')}`;
  }

  showRegister(): void {
    this.setScreen('register');
  }
  showLogin(): void {
    this.setScreen('login');
  }
  showForgotPassword(): void {
    this.emailForm.patchValue({ email: this.loginEmail.value || '' });
    this.setScreen('email');
  }
  private setScreen(screen: AuthScreen): void {
    if (screen !== 'otp') this.stopTimer();
    this.screen = screen;
    this.clearMessages();
    this.cdr.detectChanges();
  }

  login(): void {
    this.clearMessages();
    if (this.loginForm.invalid || this.isLoading) {
      this.loginForm.markAllAsTouched();
      return;
    }
    const credentials = this.loginForm.getRawValue();
    this.isLoading = true;
    this.http
      .post(`${this.API_URL}/login`, credentials, { responseType: 'text', withCredentials: true })
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: () => {
          this.storeSession(credentials.email || '');
          this.loginForm.reset();
          void this.router.navigateByUrl('/home');
        },
        error: (error: HttpErrorResponse) =>
          this.displayError(error, 'Login failed. Check your credentials.'),
      });
  }

  register(): void {
    this.clearMessages();
    if (this.registerForm.invalid || this.isLoading) {
      this.registerForm.markAllAsTouched();
      return;
    }
    this.isLoading = true;
    this.http
      .post(`${this.API_URL}/register`, this.registerForm.getRawValue(), { responseType: 'text' })
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: () => {
          const email = this.registerEmail.value || '';
          this.loginForm.patchValue({ email });
          this.registerForm.reset();
          this.setScreen('login');
          this.successMessage = 'Account created successfully. You can now sign in.';
        },
        error: (error: HttpErrorResponse) =>
          this.displayError(error, 'Registration failed. Please try again.'),
      });
  }

  sendOtp(): void {
    this.clearMessages();
    if (this.emailForm.invalid || this.isLoading) {
      this.emailForm.markAllAsTouched();
      return;
    }
    this.requestOtp(false);
  }

  resendOtp(): void {
    if (this.isLoading || this.screen !== 'otp') return;
    this.clearMessages();
    this.requestOtp(true);
  }

  private requestOtp(resend: boolean): void {
    const email = (this.emailForm.controls.email.value || '').trim();
    this.isLoading = true;
    this.http
      .post(`${this.API_URL}/forgot-password`, { email }, { responseType: 'text' })
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: () => {
          this.verifiedEmail = email;
          this.otpDigits = ['', '', '', '', '', ''];
          this.screen = 'otp';
          this.startTimer();
          this.successMessage = resend
            ? 'A new verification code has been sent.'
            : 'Verification code sent. Check your inbox.';
          this.cdr.detectChanges();
        },
        error: (error: HttpErrorResponse) =>
          this.displayError(error, 'Could not send the code. Please try again.'),
      });
  }

  onOtpInput(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;
    const digits = input.value.replace(/\D/g, '');
    if (digits.length > 1) {
      this.fillOtp(digits, index);
      return;
    }
    this.otpDigits[index] = digits;
    input.value = digits;
    if (digits && index < 5) this.focusOtp(index + 1);
  }
  onOtpKeydown(event: KeyboardEvent, index: number): void {
    if (event.key === 'Backspace' && !this.otpDigits[index] && index > 0) {
      this.otpDigits[index - 1] = '';
      this.focusOtp(index - 1);
    }
    if (event.key === 'ArrowLeft' && index > 0) this.focusOtp(index - 1);
    if (event.key === 'ArrowRight' && index < 5) this.focusOtp(index + 1);
  }
  onOtpPaste(event: ClipboardEvent): void {
    event.preventDefault();
    this.fillOtp(event.clipboardData?.getData('text') || '', 0);
  }
  private fillOtp(value: string, start: number): void {
    const digits = value.replace(/\D/g, '').slice(0, 6 - start);
    [...digits].forEach((digit, offset) => (this.otpDigits[start + offset] = digit));
    this.cdr.detectChanges();
    this.focusOtp(Math.min(5, start + digits.length));
  }
  private focusOtp(index: number): void {
    document.getElementById(`otp-${index}`)?.focus();
  }

  verifyOtp(): void {
    this.clearMessages();
    if (this.isLoading || this.otpValue.length !== 6) {
      this.errorMessage = 'Enter the complete 6-digit verification code.';
      return;
    }
    if (this.remainingSeconds <= 0) {
      this.errorMessage = 'Your code has expired. Request a new one.';
      return;
    }
    this.isLoading = true;
    this.http
      .post(
        `${this.API_URL}/forgot-password/otp/verify`,
        { email: this.verifiedEmail, otp: this.otpValue },
        { responseType: 'text', withCredentials: true },
      )
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: () => {
          this.storeSession(this.verifiedEmail);
          this.stopTimer();
          this.resetForm.reset();
          this.setScreen('reset');
        },
        error: (error: HttpErrorResponse) =>
          this.displayError(error, 'Invalid or expired code. Please try again.'),
      });
  }

  skipReset(): void {
    this.complete('You are signed in. Redirecting to Dashboard...');
  }

  submitReset(): void {
    this.clearMessages();
    if (this.resetForm.invalid || this.isLoading) {
      this.resetForm.markAllAsTouched();
      return;
    }
    const password = this.resetForm.controls.password.value || '';
    this.isLoading = true;
    this.profileService
      .resetPassword(password)
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: () => {
          this.resetForm.reset();
          this.complete('Password Reset Successful. Redirecting to Dashboard...');
        },
        error: (error: HttpErrorResponse) =>
          this.displayError(error, 'Could not reset password. Please try again.'),
      });
  }

  private complete(message: string): void {
    this.completionMessage = message;
    this.setScreen('complete');
    this.redirectTimeout = setTimeout(() => void this.router.navigateByUrl('/home'), 2500);
  }
  private startTimer(): void {
    this.stopTimer();
    this.otpExpiresAt = Date.now() + 300_000;
    this.remainingSeconds = 300;
    this.countdownInterval = setInterval(() => {
      this.remainingSeconds = Math.max(0, Math.ceil((this.otpExpiresAt - Date.now()) / 1000));
      if (!this.remainingSeconds) this.stopTimer();
      this.cdr.detectChanges();
    }, 250);
  }
  private stopTimer(): void {
    if (this.countdownInterval) clearInterval(this.countdownInterval);
    this.countdownInterval = undefined;
  }
  private storeSession(email: string): void {
    resetSessionExpirationState();

    localStorage.setItem('isLoggedIn', 'true');
    localStorage.setItem('user', JSON.stringify({ email }));
  }
  logout(): void {
    if (this.isLoggingOut) return;
    this.isLoggingOut = true;
    this.authService
      .logout()
      .pipe(
        finalize(() => {
          localStorage.removeItem('isLoggedIn');
          localStorage.removeItem('user');
          this.isLoggingOut = false;
        }),
      )
      .subscribe({
        next: () => {
          void this.router.navigateByUrl('/').then((navigated) => {
            if (navigated) {
              this.toastService.success(
                'Logged out successfully',
                'You have been securely logged out of your HelpDesk account.',
              );
            }
          });
        },
        error: () => void this.router.navigateByUrl('/'),
      });
  }
  private resetAuthScreen(): void {
    this.stopTimer();
    if (this.redirectTimeout) clearTimeout(this.redirectTimeout);
    this.screen = 'login';
    this.isLoading = false;
    this.otpDigits = ['', '', '', '', '', ''];
    this.verifiedEmail = '';
    this.loginForm.reset();
    this.registerForm.reset();
    this.emailForm.reset();
    this.resetForm.reset();
    this.clearMessages();
    this.cdr.detectChanges();
  }
  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
  private displayError(
      error: HttpErrorResponse,
      fallback: string,
  ): void {
    // Backend unavailable or network connection failure.
    if (error.status === 0) {
      this.errorMessage =
          'Unable to connect to the server. Please try again later.';
    }

    // Internal server failure.
    else if (error.status >= 500) {
      this.errorMessage =
          error.status === 503
              ? 'Service temporarily unavailable. Please try again in a few moments.'
              : 'Something went wrong on our server. Please try again later.';
    }

    // Authentication failure.
    else if (error.status === 401) {
      this.errorMessage =
          'Authentication failed. Please sign in again.';
    }

    // Authorization failure.
    else if (error.status === 403) {
      this.errorMessage =
          'You do not have permission to perform this action.';
    }

    // Other backend errors.
    else {
      const body = error.error;

      let message = fallback;

      if (typeof body === 'string' && body.trim()) {
        try {
          const parsed = JSON.parse(body);
          message =
              parsed.message ||
              parsed.error ||
              fallback;
        } catch {
          message = body.replace(/^Error:\s*/i, '');
        }
      } else if (body && typeof body === 'object') {
        message =
            body.message ||
            body.detail ||
            fallback;
      }

      this.errorMessage = message;
    }

    this.cdr.detectChanges();
  }

  ngOnDestroy(): void {
    this.stopTimer();

    if (this.redirectTimeout) {
      clearTimeout(this.redirectTimeout);
    }
  }



}
