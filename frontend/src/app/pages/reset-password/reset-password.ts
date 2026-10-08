import { Component, NgZone, ChangeDetectorRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ClarityModule } from '@clr/angular';
import { AuthService } from '../../services/auth.service';
import { TranslatePipe } from '../../pipes/translate.pipe';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule, ClarityModule, TranslatePipe],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.scss'
})
export class ResetPassword implements OnInit {
  token = '';
  newPassword = '';
  confirmPassword = '';
  isLoading = false;
  message = '';
  isError = false;
  success = false;

  constructor(
    private authService: AuthService,
    private route: ActivatedRoute,
    private router: Router,
    private ngZone: NgZone,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';
    if (!this.token) {
      this.message = 'Missing or invalid reset link.';
      this.isError = true;
    }
  }

  onSubmit(): void {
    if (!this.token) {
      this.message = 'Missing or invalid reset link.';
      this.isError = true;
      return;
    }
    if (!this.newPassword || this.newPassword.length < 6) {
      this.message = 'Password must be at least 6 characters.';
      this.isError = true;
      return;
    }
    if (this.newPassword !== this.confirmPassword) {
      this.message = 'Passwords do not match.';
      this.isError = true;
      return;
    }

    this.isLoading = true;
    this.message = '';
    this.isError = false;

    this.authService.resetPasswordConfirm({ token: this.token, newPassword: this.newPassword }).subscribe({
      next: (response) => {
        this.ngZone.run(() => {
          this.isLoading = false;
          this.success = true;
          this.message = response;
          this.cdr.detectChanges();
        });
      },
      error: (error) => {
        this.ngZone.run(() => {
          this.isLoading = false;
          this.isError = true;
          this.message = error.error || 'The reset link is invalid or has expired.';
          this.cdr.detectChanges();
        });
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
