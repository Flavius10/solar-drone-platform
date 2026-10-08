import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Farm, CreateFarmRequest, FarmDefaultsRequest, FarmNotificationSettingsRequest, FarmPricingSettingsRequest, FarmBrandingSettingsRequest } from '../models/farm.model';
import { UserSummary } from '../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class FarmService {
  private baseUrl = 'http://localhost:8080/api/farms';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Farm[]> {
    return this.http.get<Farm[]>(this.baseUrl);
  }

  create(request: CreateFarmRequest): Observable<Farm> {
    return this.http.post<Farm>(this.baseUrl, request);
  }

  updateDefaults(farmId: number, request: FarmDefaultsRequest): Observable<Farm> {
    return this.http.put<Farm>(`${this.baseUrl}/${farmId}/defaults`, request);
  }

  updateNotificationSettings(farmId: number, request: FarmNotificationSettingsRequest): Observable<Farm> {
    return this.http.put<Farm>(`${this.baseUrl}/${farmId}/notification-settings`, request);
  }

  testWebhook(farmId: number): Observable<string> {
    return this.http.post(`${this.baseUrl}/${farmId}/test-webhook`, {}, { responseType: 'text' });
  }

  updatePricingSettings(farmId: number, request: FarmPricingSettingsRequest): Observable<Farm> {
    return this.http.put<Farm>(`${this.baseUrl}/${farmId}/pricing-settings`, request);
  }

  updateBrandingSettings(farmId: number, request: FarmBrandingSettingsRequest): Observable<Farm> {
    return this.http.put<Farm>(`${this.baseUrl}/${farmId}/branding-settings`, request);
  }

  uploadLogo(farmId: number, file: File): Observable<Farm> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Farm>(`${this.baseUrl}/${farmId}/branding-logo`, formData);
  }

  removeLogo(farmId: number): Observable<Farm> {
    return this.http.delete<Farm>(`${this.baseUrl}/${farmId}/branding-logo`);
  }

  
  
  
  
  getLogoBlob(farmId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${farmId}/branding-logo`, { responseType: 'blob' });
  }

  getUsers(farmId: number): Observable<UserSummary[]> {
    return this.http.get<UserSummary[]>(`${this.baseUrl}/${farmId}/users`);
  }

  assignUser(farmId: number, username: string): Observable<string> {
    return this.http.post(`${this.baseUrl}/${farmId}/users`, { username }, { responseType: 'text' });
  }

  unassignUser(farmId: number, username: string): Observable<string> {
    return this.http.delete(`${this.baseUrl}/${farmId}/users/${username}`, { responseType: 'text' });
  }
}
