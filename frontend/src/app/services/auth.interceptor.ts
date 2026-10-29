import { inject } from '@angular/core';
import {
    HttpErrorResponse,
    HttpInterceptorFn,
} from '@angular/common/http';
import { Router } from '@angular/router';

import {
    catchError,
    finalize,
    Observable,
    shareReplay,
    switchMap,
    throwError,
} from 'rxjs';

import { AuthService } from './auth.service';
import { ToastService } from './toast.service';

const API_URL = 'http://localhost:8080/';

// Shared refresh operation to prevent concurrent token rotation.
let refreshInProgress$: Observable<void> | null = null;

// Prevent duplicate session-expiration notifications.
let sessionExpiredHandled = false;

// Prevent duplicate internal server error notifications.
let serverErrorHandled = false;

export function resetSessionExpirationState(): void {
    sessionExpiredHandled = false;
    serverErrorHandled = false;
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
    const authService = inject(AuthService);
    const router = inject(Router);
    const toastService = inject(ToastService);

    // Intercept only Spring Boot API requests.
    if (!req.url.startsWith(API_URL)) {
        return next(req);
    }

    // Include authentication cookies.
    const request = req.clone({
        withCredentials: true,
    });

    // Handle unexpected backend failures.
    const handleServerError = (error: HttpErrorResponse): void => {
        if (error.status < 500 || serverErrorHandled) {
            return;
        }

        serverErrorHandled = true;

        toastService.error(
            'Internal Server Error',
            'An unexpected error occurred. Please try again later.'
        );

        // Allow future server failures to display notifications.
        setTimeout(() => {
            serverErrorHandled = false;
        }, 5000);
    };

    // Authentication endpoints must not trigger refresh.
    // Their server errors should still be reported.
    if (req.url.startsWith(`${API_URL}auth/`)) {
        return next(request).pipe(
            catchError((error: HttpErrorResponse) => {
                handleServerError(error);
                return throwError(() => error);
            }),
        );
    }

    // Handle an unrecoverable authentication failure once.
    const handleSessionExpired = (): void => {
        if (sessionExpiredHandled) return;

        sessionExpiredHandled = true;

        localStorage.removeItem('isLoggedIn');
        localStorage.removeItem('user');

        void router.navigateByUrl('/').then(
            () => toastService.sessionExpired(),
            () => toastService.sessionExpired(),
        );
    };

    return next(request).pipe(
        catchError((originalError: HttpErrorResponse) => {

            // Preserve network and server errors.
            if (originalError.status !== 401) {
                handleServerError(originalError);
                return throwError(() => originalError);
            }

            // Session expiration already handled.
            if (sessionExpiredHandled) {
                return throwError(() => originalError);
            }

            // Start or reuse a shared refresh operation.
            if (!refreshInProgress$) {
                refreshInProgress$ = authService
                    .refreshAccessToken()
                    .pipe(
                        finalize(() => {
                            refreshInProgress$ = null;
                        }),
                        shareReplay({
                            bufferSize: 1,
                            refCount: false,
                        }),
                    );
            }

            return refreshInProgress$.pipe(

                // Handle refresh failures separately.
                catchError((refreshError: HttpErrorResponse) => {

                    // Only authentication-related refresh failures
                    // should expire the session.
                    if (
                        refreshError.status === 401 ||
                        refreshError.status === 403
                    ) {
                        handleSessionExpired();
                    } else {
                        handleServerError(refreshError);
                    }

                    // Preserve the actual refresh error.
                    return throwError(() => refreshError);
                }),

                // Refresh succeeded; retry original request once.
                switchMap(() =>
                    next(request).pipe(
                        catchError((retryError: HttpErrorResponse) => {

                            // Authentication still fails after refresh.
                            if (retryError.status === 401) {
                                handleSessionExpired();
                            } else {
                                handleServerError(retryError);
                            }

                            // Preserve actual retry errors.
                            return throwError(() => retryError);
                        }),
                    ),
                ),
            );
        }),
    );
};
