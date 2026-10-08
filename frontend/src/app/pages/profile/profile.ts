import { Component, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ClarityModule } from '@clr/angular';
import { AuthService } from '../../services/auth.service';
import { TranslatePipe } from '../../pipes/translate.pipe';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, ClarityModule, TranslatePipe],
  templateUrl: './profile.html',
  styleUrl: './profile.scss'
})
export class Profile {
  email = '';
  isLoading = false;
  message = '';
  isError = false;

  constructor(public authService: AuthService, private ngZone: NgZone, private cdr: ChangeDetectorRef) {}

  onSubmit(): void {
    if (!this.email) {
      this.message = 'Email is required.';
      this.isError = true;
      return;
    }

    this.isLoading = true;
    this.message = '';
    this.isError = false;

    this.authService.updateEmail({ email: this.email }).subscribe({
      next: (response) => {
        this.ngZone.run(() => {
          this.isLoading = false;
          this.message = response;
          this.isError = false;
          this.cdr.detectChanges();
        });
      },
      error: (error) => {
        this.ngZone.run(() => {
          this.isLoading = false;
          this.isError = true;
          this.message = typeof error.error === 'string' ? error.error : 'Failed to update email.';
          this.cdr.detectChanges();
        });
      }
    });
  }
}
