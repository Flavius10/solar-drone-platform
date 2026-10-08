import { Component, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ClarityModule } from '@clr/angular';
import { AuthService } from '../../services/auth.service';
import { TranslatePipe } from '../../pipes/translate.pipe';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, ClarityModule, RouterLink, TranslatePipe],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login {
  username = '';
  password = '';
  isLoading = false;
  errorMessage = '';

  constructor(private authService: AuthService, private router: Router, private ngZone: NgZone, private cdr: ChangeDetectorRef) {}

  onLogin(): void {
  if (!this.username || !this.password) {
    this.errorMessage = 'Please enter both username and password.';
    return;
  }

  this.isLoading = true;
  this.errorMessage = '';

  this.authService.login({ username: this.username, password: this.password }).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.isLoading = false;
          this.router.navigate(['/dashboard']);
          this.cdr.detectChanges();
        });
      },
      error: (error) => {
        this.ngZone.run(() => {
          this.isLoading = false;
          if (error.status === 401) {
            this.errorMessage = 'Invalid username or password.';
          } else if (error.status === 423) {
            this.errorMessage = 'Account locked due to too many failed attempts. Try again in 15 minutes, or reset your password below.';
          } else {
            this.errorMessage = 'Login failed. Is the Java server running?';
          }
          this.cdr.detectChanges();
        });
      }
    });
  }
}