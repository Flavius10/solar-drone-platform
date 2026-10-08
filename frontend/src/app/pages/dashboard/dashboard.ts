import { Component, OnInit, OnDestroy, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { DashboardStats, InspectionReport } from '../../models/inspection.model';
import { InspectionService } from '../../services/inspection.service';
import { ReportService } from '../../services/report.service';
import { PanelService } from '../../services/panel.service';
import { FarmContextService } from '../../services/farm-context.service';
import { LiveEventsService } from '../../services/live-events.service';
import { CostPipe } from '../../pipes/cost.pipe';
import { TranslatePipe } from '../../pipes/translate.pipe';

interface HealthBucket {
  key: string;
  label: string;
  count: number;
  pct: number;
  colorClass: string;
}

interface DayBucket {
  label: string;
  count: number;
}

interface CostBucket {
  defectType: string;
  label: string;
  cost: number;
  pct: number;
}

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, CostPipe, TranslatePipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit, OnDestroy {
  stats: DashboardStats | null = null;
  loadError = '';
  healthBuckets: HealthBucket[] = [];
  recentInspections: InspectionReport[] = [];
  weeklyBuckets: DayBucket[] = [];
  maxWeeklyCount = 1;
  costBuckets: CostBucket[] = [];

  private currentFarmId: number | null = null;
  private farmSub?: Subscription;
  private liveSub?: Subscription;

    constructor(
      private inspectionService: InspectionService,
      private ngZone: NgZone,
      private cdr: ChangeDetectorRef,
      private reportService: ReportService,
      private panelService: PanelService,
      private farmContext: FarmContextService,
      private liveEvents: LiveEventsService
    ) {}

    ngOnInit(): void {
    this.farmSub = this.farmContext.selectedFarmId$.subscribe(farmId => {
      this.currentFarmId = farmId;
      this.loadStats(farmId);
      this.loadHealthBreakdown(farmId);
      this.loadRecentAndWeekly(farmId);
    });

    
    
    this.liveSub = this.liveEvents.events$.subscribe(event => {
      if (event.type === 'INSPECTION_STATUS' || event.type === 'INSPECTION_COMPLETE' || event.type === 'INSPECTION_FAILED') {
        this.loadStats(this.currentFarmId);
        this.loadHealthBreakdown(this.currentFarmId);
        this.loadRecentAndWeekly(this.currentFarmId);
      }
    });
  }

  ngOnDestroy(): void {
    this.farmSub?.unsubscribe();
    this.liveSub?.unsubscribe();
  }

  loadStats(farmId: number | null): void {
    this.loadError = '';
    this.inspectionService.getDashboardStats(farmId).subscribe({
      next: (data) => {
        this.ngZone.run(() => {
          this.stats = data;
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          console.error('Failed to load stats', err);
          this.loadError = err.status === 403
            ? "You don't have access to this farm."
            : 'Failed to load dashboard stats.';
          this.cdr.detectChanges();
        });
      }
    });
  }

  
  
  
  loadHealthBreakdown(farmId: number | null): void {
    this.panelService.getGrid(farmId).subscribe({
      next: (panels) => {
        this.ngZone.run(() => {
          const total = panels.length || 1;
          const counts: Record<string, number> = { OK: 0, DEFECT: 0, PROCESSING: 0, NO_DATA: 0 };
          for (const p of panels) {
            const key = counts.hasOwnProperty(p.latestStatus) ? p.latestStatus : 'NO_DATA';
            counts[key]++;
          }
          this.healthBuckets = [
            { key: 'OK', label: 'Healthy (OK)', count: counts['OK'], pct: (counts['OK'] / total) * 100, colorClass: 'bucket-ok' },
            { key: 'DEFECT', label: 'Defect Detected', count: counts['DEFECT'], pct: (counts['DEFECT'] / total) * 100, colorClass: 'bucket-defect' },
            { key: 'PROCESSING', label: 'In Progress', count: counts['PROCESSING'], pct: (counts['PROCESSING'] / total) * 100, colorClass: 'bucket-processing' },
            { key: 'NO_DATA', label: 'No Data', count: counts['NO_DATA'], pct: (counts['NO_DATA'] / total) * 100, colorClass: 'bucket-nodata' },
          ];
          this.cdr.detectChanges();
        });
      },
      error: () => {}
    });
  }

  
  
  
  loadRecentAndWeekly(farmId: number | null): void {
    this.inspectionService.getReportsHistory(farmId).subscribe({
      next: (list) => {
        this.ngZone.run(() => {
          const sorted = [...list].sort((a, b) => {
            const dateDiff = new Date(b.inspectionDate).getTime() - new Date(a.inspectionDate).getTime();
            return dateDiff !== 0 ? dateDiff : (b.id || 0) - (a.id || 0);
          });
          this.recentInspections = sorted.slice(0, 6);

          const buckets: DayBucket[] = [];
          for (let i = 6; i >= 0; i--) {
            const d = new Date();
            d.setDate(d.getDate() - i);
            const key = d.toISOString().slice(0, 10);
            const label = d.toLocaleDateString(undefined, { weekday: 'short' });
            const count = list.filter(r => r.inspectionDate === key).length;
            buckets.push({ label, count });
          }
          this.weeklyBuckets = buckets;
          this.maxWeeklyCount = Math.max(1, ...buckets.map(b => b.count));
          this.costBuckets = this.buildCostBreakdown(list);
          this.cdr.detectChanges();
        });
      },
      error: () => {}
    });
  }

  
  
  
  
  
  
  private buildCostBreakdown(list: InspectionReport[]): CostBucket[] {
    const totals = new Map<string, number>();
    for (const r of list) {
      if (!r.defectType || r.defectType === 'NONE') continue;
      const cost = r.estimatedRepairCost || 0;
      totals.set(r.defectType, (totals.get(r.defectType) || 0) + cost);
    }

    const entries = Array.from(totals.entries()).sort((a, b) => b[1] - a[1]);
    const max = Math.max(1, ...entries.map(([, cost]) => cost));

    return entries.map(([defectType, cost]) => ({
      defectType,
      label: this.prettifyDefectType(defectType),
      cost,
      pct: (cost / max) * 100
    }));
  }

  private prettifyDefectType(defectType: string): string {
    return defectType
      .toLowerCase()
      .split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  downloadPdf(): void {
    this.reportService.exportPdf(this.currentFarmId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'farm-health-report.pdf';
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => console.error('Failed to export PDF', err)
    });
  }

}
