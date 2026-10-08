import { Component, OnInit, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ClarityModule } from '@clr/angular';
import { AuthService } from '../../services/auth.service';
import { UserSummary } from '../../models/auth.model';
import { TranslatePipe } from '../../pipes/translate.pipe';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule, ClarityModule, TranslatePipe],
  templateUrl: './user-management.html',
  styleUrl: './user-management.scss'
})
export class UserManagement implements OnInit {
  username = '';
  password = '';
  role = 'OPERATOR';
  subscriptionTier = 'BASIC';

  roles = ['ADMIN', 'OPERATOR', 'TECHNICIAN', 'VIEWER'];
  tiers = ['BASIC', 'PRO', 'ENTERPRISE'];

  isLoading = false;
  message = '';
  isError = false;

  users: UserSummary[] = [];
  isLoadingUsers = false;

  constructor(private authService: AuthService, private ngZone: NgZone, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.isLoadingUsers = true;
    this.authService.getUsers().subscribe({
      next: (data) => {
        this.ngZone.run(() => {
          this.users = data;
          this.isLoadingUsers = false;
          this.cdr.detectChanges();
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.isLoadingUsers = false;
          this.cdr.detectChanges();
        });
      }
    });
  }

  onSubmit(): void {
    if (!this.username || !this.password) {
      this.message = 'Username and password are required.';
      this.isError = true;
      return;
    }

    this.isLoading = true;
    this.message = '';
    this.isError = false;

    this.authService.registerAdmin({
      username: this.username,
      password: this.password,
      role: this.role,
      subscriptionTier: this.subscriptionTier
    }).subscribe({
      next: (response) => {
        this.ngZone.run(() => {
          this.isLoading = false;
          this.message = response;
          this.isError = false;
          this.username = '';
          this.password = '';
          this.loadUsers();
          this.cdr.detectChanges();
        });
      },
      error: (error) => {
        this.ngZone.run(() => {
          this.isLoading = false;
          this.isError = true;
          this.message = typeof error.error === 'string' ? error.error : 'Failed to create user.';
          this.cdr.detectChanges();
        });
      }
    });
  }
}
