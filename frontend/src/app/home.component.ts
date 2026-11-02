import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from './services/auth.service';
import { ProfileService, ProfileResponse } from './services/profile.service';
import { ToastService } from './services/toast.service';
import { OrganizationService } from './services/organization.service';

import { OrganizationRequest, OrganizationResponse } from './models/organization.model';

interface ProfileField {
  label: string;
  value: string;
}

interface NavigationItem {
  id: string;
  label: string;
  icon: string;
  description: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
})
export class HomeComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);
  private readonly organizationService = inject(OrganizationService);

  constructor(
    private readonly router: Router,
    private readonly profileService: ProfileService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  // -----------------------------
  // DASHBOARD
  // -----------------------------

  activeSection = 'overview';
  activeOrganization: string | null = null;
  isSidebarOpen = false;

  readonly generalNavigation: NavigationItem[] = [
    {
      id: 'overview',
      label: 'Overview',
      icon: '▦',
      description: 'Your support workspace overview.',
    },
    {
      id: 'tickets',
      label: 'Tickets',
      icon: '▤',
      description: 'Create, search, assign and resolve tickets.',
    },
    {
      id: 'inbox',
      label: 'Team inbox',
      icon: '✉',
      description: 'Manage public replies and internal notes.',
    },
    {
      id: 'reports',
      label: 'Reports & analytics',
      icon: '◫',
      description: 'Analyze support performance and ticket metrics.',
    },
  ];

  readonly managementNavigation: NavigationItem[] = [
    {
      id: 'organizations',
      label: 'Organizations',
      icon: '▧',
      description: 'Create and manage organizations.',
    },
    {
      id: 'members',
      label: 'Members & roles',
      icon: '♙',
      description: 'Manage team memberships and permissions.',
    },
    {
      id: 'invitations',
      label: 'Invitations',
      icon: '✉',
      description: 'Invite team members to your workspace.',
    },
    {
      id: 'sla',
      label: 'SLA policies',
      icon: '◷',
      description: 'Configure SLA targets and escalations.',
    },
    {
      id: 'notifications',
      label: 'Notifications',
      icon: '♧',
      description: 'Manage support notifications.',
    },
    {
      id: 'audit',
      label: 'Audit history',
      icon: '☷',
      description: 'Review organization activity.',
    },
    {
      id: 'settings',
      label: 'Settings',
      icon: '⚙',
      description: 'Manage workspace settings.',
    },
  ];

  readonly metrics = [
    {
      label: 'Total tickets',
      icon: '▤',
      tone: 'blue',
      description: 'All tickets in your workspace',
    },
    {
      label: 'Open tickets',
      icon: '◷',
      tone: 'amber',
      description: 'Awaiting attention',
    },
    {
      label: 'Resolved tickets',
      icon: '✓',
      tone: 'green',
      description: 'Successfully resolved',
    },
    {
      label: 'Overdue tickets',
      icon: '!',
      tone: 'red',
      description: 'Past applicable SLA deadlines',
    },
  ];

  readonly setupSteps = [
    {
      title: 'Create an organization',
      description: 'Establish your support workspace.',
    },
    {
      title: 'Invite your team',
      description: 'Add agents and administrators.',
    },
    {
      title: 'Start managing tickets',
      description: 'Receive and resolve support requests.',
    },
    {
      title: 'Configure SLA policies',
      description: 'Define support targets and escalation rules.',
    },
  ];

  get selectedNavigation(): NavigationItem | undefined {
    return [...this.generalNavigation, ...this.managementNavigation].find(
      (item) => item.id === this.activeSection,
    );
  }

  get sectionTitle(): string {
    return this.selectedNavigation?.label ?? 'Overview';
  }

  get sectionDescription(): string {
    return this.selectedNavigation?.description ?? '';
  }

  selectSection(section: string): void {
    this.activeSection = section;
    this.isSidebarOpen = false;
    this.isProfileOpen = false;
  }

  // -----------------------------
  // USER / PROFILE
  // -----------------------------

  userName = 'User';
  userEmail = '';
  userInitial = 'U';

  isProfileOpen = false;
  isProfileModalOpen = false;
  isProfileLoading = false;

  profileError = '';
  profileDetails: ProfileResponse | null = null;

  isLoggingOut = false;
  logoutError = '';

  ngOnInit(): void {
    this.loadUser();
    this.loadAuthenticatedUser();
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

  private loadAuthenticatedUser(): void {
    this.profileService.getUserDetails().subscribe({
      next: (response: ProfileResponse) => {
        const profile = response as Record<string, unknown>;

        const name = profile['name'];
        const email = profile['email'];

        if (typeof email === 'string' && email.trim()) {
          this.userEmail = email.trim();
        }

        if (typeof name === 'string' && name.trim()) {
          this.userName = name.trim();
        } else if (this.userEmail) {
          this.userName = this.userEmail;
        }

        this.userInitial = this.userName.charAt(0).toUpperCase();

        this.cdr.detectChanges();
      },
      error: () => {
        // The dashboard remains accessible.
        // Existing authentication interceptor handles
        // session-related failures.
      },
    });
  }

  toggleProfile(): void {
    this.isProfileOpen = !this.isProfileOpen;
    this.cdr.detectChanges();
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

  get profileInitial(): string {
    const profile = this.profileDetails as
      | (ProfileResponse & {
          name?: string;
          email?: string;
        })
      | null;

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

  // -----------------------------
  // ORGANIZATION CREATION
  // -----------------------------

  isOrganizationFormOpen = false;
  isCreatingOrganization = false;

  organizationError = '';

  createdOrganization: OrganizationResponse | null = null;

  organizationForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    description: [''],
    slug: ['', [Validators.required, Validators.pattern(/^[a-z0-9]+(?:-[a-z0-9]+)*$/)]],
    contactEmail: ['', Validators.email],
    contactPhone: [''],
    websiteUrl: [''],
    logoUrl: [''],
    address: [''],
  });

  openOrganizationForm(): void {
    this.organizationError = '';
    this.organizationForm.reset();

    this.isOrganizationFormOpen = true;
    this.isSidebarOpen = false;
    this.isProfileOpen = false;
  }

  closeOrganizationForm(): void {
    if (this.isCreatingOrganization) {
      return;
    }

    this.isOrganizationFormOpen = false;
    this.organizationError = '';
    this.organizationForm.reset();
  }

  createOrganization(): void {
    if (this.isCreatingOrganization) {
      return;
    }

    this.organizationError = '';
    this.organizationForm.markAllAsTouched();

    if (this.organizationForm.invalid) {
      return;
    }

    const creatorEmail = this.userEmail.trim();

    if (!creatorEmail) {
      this.organizationError =
        'Unable to determine your account email. Please reload your profile and try again.';
      return;
    }

    const values = this.organizationForm.getRawValue();

    const request: OrganizationRequest = {
      name: values.name.trim(),
      description: values.description.trim(),
      slug: values.slug.trim().toLowerCase(),
      contactEmail: values.contactEmail.trim(),
      contactPhone: values.contactPhone.trim(),
      websiteUrl: values.websiteUrl.trim(),
      logoUrl: values.logoUrl.trim(),
      address: values.address.trim(),
      creatorEmail,
    };

    if (!request.name || !request.slug) {
      this.organizationError = 'Organization name and slug are required.';
      return;
    }

    this.isCreatingOrganization = true;

    this.organizationService
      .createOrganization(request)
      .pipe(
        finalize(() => {
          this.isCreatingOrganization = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (response: OrganizationResponse) => {
          this.createdOrganization = response;

          this.activeOrganization = response.name || request.name;

          this.isOrganizationFormOpen = false;
          this.organizationForm.reset();

          this.activeSection = 'overview';

          this.toastService.success(
            'Organization created successfully',
            `${this.activeOrganization} is ready for setup.`,
          );

          this.cdr.detectChanges();
        },

        error: (error: HttpErrorResponse) => {
          this.organizationError = this.getOrganizationErrorMessage(error);

          this.cdr.detectChanges();
        },
      });
  }

  private getOrganizationErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'Unable to connect to the server. Please try again.';
    }

    if (error.status >= 500) {
      return 'Something went wrong on our server. Please try again later.';
    }

    if (error.status === 401) {
      return 'Your session has expired. Please sign in again.';
    }

    if (error.status === 403) {
      return 'You do not have permission to create an organization.';
    }

    if (error.status === 409) {
      return 'An organization with these details may already exist.';
    }

    const body = error.error;

    if (typeof body === 'string' && body.trim()) {
      return body;
    }

    if (body && typeof body === 'object' && typeof body.message === 'string') {
      return body.message;
    }

    return 'Unable to create organization. Please check your details.';
  }

  // -----------------------------
  // PASSWORD RESET
  // -----------------------------

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
      .pipe(
        finalize(() => {
          this.isVerifyingCurrentPassword = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: () => {
          this.isCurrentPasswordVerified = true;
          this.currentPasswordError = '';

          this.resetPasswordForm.reset();
          this.showNewPassword = false;
          this.showConfirmPassword = false;

          this.cdr.detectChanges();
        },
        error: (error: HttpErrorResponse) => {
          this.currentPasswordError = this.getBackendErrorMessage(
            error,
            'Unable to verify your current password. Please try again.',
          );

          this.cdr.detectChanges();
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

      case 'lowercase':
        return (password.match(/[a-z]/g) ?? []).length >= 3;

      case 'number':
        return (password.match(/[0-9]/g) ?? []).length >= 2;

      case 'special':
        return /[!@#$&*]/.test(password);

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
      .pipe(
        finalize(() => {
          this.isResettingPassword = false;
          this.cdr.detectChanges();
        }),
      )
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

          this.cdr.detectChanges();
        },
        error: (error: HttpErrorResponse) => {
          this.resetPasswordError = this.getBackendErrorMessage(
            error,
            'Unable to reset your password. Please try again.',
          );

          this.cdr.detectChanges();
        },
      });
  }

  // -----------------------------
  // LOGOUT
  // -----------------------------

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
          this.cdr.detectChanges();
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

    this.activeOrganization = null;
    this.createdOrganization = null;

    this.isProfileOpen = false;
    this.isProfileModalOpen = false;

    this.profileDetails = null;
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
}
