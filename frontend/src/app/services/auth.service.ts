import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { JwtResponse, LoginRequest, ForgotPasswordRequest, ResetPasswordConfirmRequest, RegisterRequest, UserSummary, UpdateEmailRequest } from '../models/auth.model';

const TIER_KEY = 'auth_tier';
const TOKEN_KEY = 'auth_token';
const USERNAME_KEY = 'auth_username';
const ROLE_KEY = 'auth_role';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private baseUrl = 'http://localhost:8080/api/auth';

  
  
  private loggedInSubject = new BehaviorSubject<boolean>(this.isLoggedIn());
  loggedIn$ = this.loggedInSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {}

  login(credentials: LoginRequest): Observable<JwtResponse> {
    return this.http.post<JwtResponse>(`${this.baseUrl}/login`, credentials).pipe(
     tap((response) => {
        localStorage.setItem(TOKEN_KEY, response.token);
        localStorage.setItem(USERNAME_KEY, response.username);
        localStorage.setItem(ROLE_KEY, response.role);
        localStorage.setItem(TIER_KEY, response.subscriptionTier);
        this.loggedInSubject.next(true);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USERNAME_KEY);
    localStorage.removeItem(ROLE_KEY);
    localStorage.removeItem(TIER_KEY);
    this.loggedInSubject.next(false);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  getUsername(): string | null {
    return localStorage.getItem(USERNAME_KEY);
  }

  getRole(): string | null {
    return localStorage.getItem(ROLE_KEY);
  }

  getSubscriptionTier(): string | null {
    return localStorage.getItem(TIER_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<string> {
    return this.http.post(`${this.baseUrl}/forgot-password`, request, { responseType: 'text' });
  }

  resetPasswordConfirm(request: ResetPasswordConfirmRequest): Observable<string> {
    return this.http.post(`${this.baseUrl}/reset-password-confirm`, request, { responseType: 'text' });
  }

  registerAdmin(request: RegisterRequest): Observable<string> {
    return this.http.post(`${this.baseUrl}/register-admin`, request, { responseType: 'text' });
  }

  getUsers(): Observable<UserSummary[]> {
    return this.http.get<UserSummary[]>(`${this.baseUrl}/users`);
  }

  updateEmail(request: UpdateEmailRequest): Observable<string> {
    return this.http.put(`${this.baseUrl}/update-email`, request, { responseType: 'text' });
  }
}