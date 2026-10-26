import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8080/auth';

  logout(): Observable<void> {
    return this.http.post<void>(
      `${this.API_URL}/logout`,
      {},
      {
        withCredentials: true,
      },
    );
  }

refreshAccessToken(): Observable<void> {
  return this.http.post<void>(
    `${this.API_URL}/refresh`,
    {},
    {
      withCredentials: true,
    }
  );
}
}
