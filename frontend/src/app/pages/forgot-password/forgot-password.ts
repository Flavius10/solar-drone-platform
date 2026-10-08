import { Component, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ClarityModule } from '@clr/angular';
import { AuthService } from '../../services/auth.service';
import { TranslatePipe } from '../../pipes/translate.pipe';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, ClarityModule, RouterLink, TranslatePipe],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.scss'
})
export class ForgotPassword {
  username = '';
  isLoading = false;
  message = '';
  submitted = false;

  constructor(private authService: AuthService, private ngZone: NgZone, private cdr: ChangeDetectorRef) {}

  onSubmit(): void {
    if (!this.username) {
      this.message = 'Please enter your username.';
      return;
    }

    this.isLoading = true;
    this.message = '';

    this.authService.forgotPassword({ username: this.username }).subscribe({
      next: (response) => {
        this.ngZone.run(() => {
          this.isLoading = false;
          this.submitted = true;
          this.message = response;
          this.cdr.detectChanges();
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.isLoading = false;
          this.message = 'Something went wrong. Please try again later.';
          this.cdr.detectChanges();
        });
      }
    });
  }
}
