import { Component, OnInit, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClarityModule } from '@clr/angular';
import { AuditLogService } from '../../services/audit-log.service';
import { AuditLogEntry } from '../../models/audit-log.model';
import { TranslatePipe } from '../../pipes/translate.pipe';

@Component({
  selector: 'app-audit-log',
  standalone: true,
  imports: [CommonModule, ClarityModule, TranslatePipe],
  templateUrl: './audit-log.html',
  styleUrl: './audit-log.scss'
})
export class AuditLog implements OnInit {
  logs: AuditLogEntry[] = [];
  isLoading = true;
  accessDenied = false;

  currentPage = 0;
  pageSize = 20;
  totalPages = 0;
  totalElements = 0;

  constructor(private auditLogService: AuditLogService, private ngZone: NgZone, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loadPage(0);
  }

  loadPage(page: number): void {
    this.isLoading = true;
    this.auditLogService.getAll(page, this.pageSize).subscribe({
      next: (data) => {
        this.ngZone.run(() => {
          this.logs = data.content;
          this.currentPage = data.number;
          this.totalPages = data.totalPages;
          this.totalElements = data.totalElements;
          this.isLoading = false;
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          console.error('Failed to load audit log', err);
          this.isLoading = false;
          if (err.status === 403) {
            this.accessDenied = true;
          }
          this.cdr.detectChanges();
        });
      }
    });
  }

  nextPage(): void {
    if (this.currentPage + 1 < this.totalPages) {
      this.loadPage(this.currentPage + 1);
    }
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.loadPage(this.currentPage - 1);
    }
  }
}