import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface ToastMessage {
  type: ToastType;
  title: string;
  message: string;
}

@Injectable({
  providedIn: 'root',
})
export class ToastService {
  readonly currentToast = signal<ToastMessage | null>(null);

  private timeoutId: ReturnType<typeof setTimeout> | null = null;

  show(type: ToastType, title: string, message: string, duration = 5000): void {
    this.dismiss();

    this.currentToast.set({
      type,
      title,
      message,
    });

    this.timeoutId = setTimeout(() => {
      this.dismiss();
    }, duration);
  }

  success(title: string, message: string): void {
    this.show('success', title, message);
  }

  error(title: string, message: string): void {
    this.show('error', title, message);
  }

  warning(title: string, message: string): void {
    this.show('warning', title, message);
  }

  info(title: string, message: string): void {
    this.show('info', title, message);
  }

  sessionExpired(): void {
    this.warning('Session expired', 'Your session has expired. Please log in again to continue.');
  }

  dismiss(): void {
    if (this.timeoutId !== null) {
      clearTimeout(this.timeoutId);
      this.timeoutId = null;
    }

    this.currentToast.set(null);
  }
}
