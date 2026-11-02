import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { OrganizationRequest, OrganizationResponse } from '../models/organization.model';

@Injectable({
  providedIn: 'root',
})
export class OrganizationService {
  private readonly http = inject(HttpClient);

  private readonly API_URL = 'http://localhost:8080/organization';

  createOrganization(request: OrganizationRequest): Observable<OrganizationResponse> {
    return this.http.post<OrganizationResponse>(`${this.API_URL}/create`, request, {
      withCredentials: true,
    });
  }
}
