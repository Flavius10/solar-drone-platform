import { Component, OnInit, OnDestroy, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClarityModule } from '@clr/angular';
import { Subscription } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { InspectionService } from '../../services/inspection.service';
import { InspectionReport } from '../../models/inspection.model';
import { ReportService } from '../../services/report.service';
import { FarmContextService } from '../../services/farm-context.service';
import { LiveEventsService } from '../../services/live-events.service';
import { AuthService } from '../../services/auth.service';
import { CostPipe } from '../../pipes/cost.pipe';
import { TranslatePipe } from '../../pipes/translate.pipe';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, FormsModule, ClarityModule, CostPipe, TranslatePipe],
  templateUrl: './reports.html',
  styleUrl: './reports.scss'
})
export class Reports implements OnInit, OnDestroy {
  reportsList: InspectionReport[] = [];
  loadError = '';
  private currentFarmId: number | null = null;
  private farmSub?: Subscription;
  private liveSub?: Subscription;

  
  
  
  searchQuery = '';
  statusFilter: 'ALL' | 'OK' | 'DEFECT' | 'PENDING' = 'ALL';
  onlyMine = false;

  constructor(
    private inspectionService: InspectionService,
    private ngZone: NgZone,
    private cdr: ChangeDetectorRef,
    private reportService: ReportService,
    private farmContext: FarmContextService,
    private liveEvents: LiveEventsService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.farmSub = this.farmContext.selectedFarmId$.subscribe(farmId => {
      this.currentFarmId = farmId;
      this.loadReports(farmId);
    });

    
    
    this.liveSub = this.liveEvents.events$.subscribe(event => {
      if (event.type === 'INSPECTION_STATUS' || event.type === 'INSPECTION_COMPLETE' || event.type === 'INSPECTION_FAILED') {
        this.loadReports(this.currentFarmId);
      }
    });
  }

  ngOnDestroy(): void {
    this.farmSub?.unsubscribe();
    this.liveSub?.unsubscribe();
  }

  loadReports(farmId: number | null): void {
    this.loadError = '';
    this.inspectionService.getReportsHistory(farmId).subscribe({
      next: (data) => {
        this.ngZone.run(() => {
          this.reportsList = data;
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          console.error('Failed to load reports', err);
          this.loadError = err.status === 403
            ? "You don't have access to this farm."
            : 'Failed to load reports.';
          this.cdr.detectChanges();
        });
      }
    });
  }

  get filteredReports(): InspectionReport[] {
    const username = (this.authService.getUsername() || '').toLowerCase();
    const query = this.searchQuery.trim().toLowerCase();

    return this.reportsList.filter(r => {
      if (this.onlyMine && (r.uploadedBy || '').toLowerCase() !== username) {
        return false;
      }
      if (this.statusFilter !== 'ALL') {
        const effectiveStatus = r.status || r.processingStatus || 'PENDING';
        if (this.statusFilter === 'PENDING') {
          if (r.status === 'OK' || r.status === 'DEFECT') return false;
        } else if (effectiveStatus !== this.statusFilter) {
          return false;
        }
      }
      if (query) {
        const haystack = [r.fileName, r.defectType, r.uploadedBy, r.inspectionType]
          .filter(Boolean).join(' ').toLowerCase();
        if (!haystack.includes(query)) return false;
      }
      return true;
    });
  }

  clearFilters(): void {
    this.searchQuery = '';
    this.statusFilter = 'ALL';
    this.onlyMine = false;
  }

  downloadExcel(): void {
    this.reportService.exportExcel(this.currentFarmId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'inspection-history.xlsx';
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => console.error('Failed to export Excel', err)
    });
  }
}