import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  return next(req).pipe(
    catchError((error) => {
      const isLoginRequest = req.url.includes('/api/auth/login');
      
      
      const isSessionError = error.status === 401;

      if (isSessionError && !isLoginRequest) {
        authService.logout();
      }

      return throwError(() => error);
    })
  );
};