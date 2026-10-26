import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Router } from '@angular/router';

import { catchError, finalize, Observable, shareReplay, switchMap, throwError } from 'rxjs';

import { AuthService } from './auth.service';

const API_URL = 'http://localhost:8080/';

// Shared refresh operation.
// Prevents multiple concurrent 401 responses from
// rotating the same refresh token multiple times.
let refreshInProgress$: Observable<void> | null = null;

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Only intercept requests to our Spring Boot backend.
  if (!req.url.startsWith(API_URL)) {
    return next(req);
  }

  // Ensure browser cookies accompany API requests.
  const request = req.clone({
    withCredentials: true,
  });

  // Authentication endpoints should not trigger refresh.
  // This includes login, registration, OTP, logout
  // and the refresh endpoint itself.
  if (req.url.startsWith(`${API_URL}auth/`)) {
    return next(request);
  }

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      // Refresh only when authentication fails.
      if (error.status !== 401) {
        return throwError(() => error);
      }

      if (!refreshInProgress$) {
        refreshInProgress$ = authService.refreshAccessToken().pipe(
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
        // Refresh succeeded. Browser now has new cookies.
        // Retry the original request once.
        switchMap(() => next(request)),

        catchError((refreshOrRetryError: HttpErrorResponse) => {
          if (refreshOrRetryError.status === 401) {
            localStorage.removeItem('isLoggedIn');
            localStorage.removeItem('user');

            void router.navigateByUrl('/');
          }

          return throwError(() => refreshOrRetryError);
        }),
      );
    }),
  );
};
