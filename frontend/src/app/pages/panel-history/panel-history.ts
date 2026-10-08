import { Component, OnInit, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ClarityModule } from '@clr/angular';
import { PanelService } from '../../services/panel.service';
import { InspectionReport } from '../../models/inspection.model';
import { CostPipe } from '../../pipes/cost.pipe';
import { TranslatePipe } from '../../pipes/translate.pipe';

interface ChartPoint {
  x: number;
  statusY: number;
  costY: number;
  date: string;
  status: string;
  defectType: string;
  cost: number;
  cumulativeCost: number;
}

const CHART_WIDTH_PER_POINT = 90;
const CHART_LEFT_PADDING = 60;
const CHART_TOP = 30;
const STATUS_ROW_Y = 40;
const COST_CHART_TOP = 100;
const COST_CHART_BOTTOM = 300;
const CHART_HEIGHT = 340;

@Component({
  selector: 'app-panel-history',
  standalone: true,
  imports: [CommonModule, ClarityModule, RouterLink, CostPipe, TranslatePipe],
  templateUrl: './panel-history.html',
  styleUrl: './panel-history.scss'
})
export class PanelHistory implements OnInit {
  panelId: number | null = null;
  history: InspectionReport[] = [];
  isLoading = false;
  errorMessage = '';

  points: ChartPoint[] = [];
  costPolyline = '';
  statusPolyline = '';
  chartWidth = 600;
  chartHeight = CHART_HEIGHT;
  maxCumulativeCost = 0;

  totalDefects = 0;
  totalInspections = 0;
  totalCost = 0;
  currency = 'USD';

  constructor(
    private panelService: PanelService,
    private route: ActivatedRoute,
    private ngZone: NgZone,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const panelIdParam = this.route.snapshot.queryParamMap.get('panelId');
    this.panelId = panelIdParam ? Number(panelIdParam) : null;

    if (!this.panelId) {
      this.errorMessage = 'No panel selected.';
      return;
    }

    this.loadHistory();
  }

  loadHistory(): void {
    if (!this.panelId) return;

    this.isLoading = true;
    this.panelService.getHistory(this.panelId).subscribe({
      next: (data) => {
        this.ngZone.run(() => {
          this.history = data;
          this.buildChart();
          this.isLoading = false;
          this.cdr.detectChanges();
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.isLoading = false;
          this.errorMessage = 'Failed to load inspection history for this panel.';
          this.cdr.detectChanges();
        });
      }
    });
  }

  private buildChart(): void {
    this.totalInspections = this.history.length;
    this.totalDefects = this.history.filter(h => h.status === 'DEFECT').length;
    this.totalCost = this.history.reduce((sum, h) => sum + (h.estimatedRepairCost || 0), 0);
    this.currency = this.history.find(h => h.costCurrency)?.costCurrency || 'USD';

    if (this.history.length === 0) {
      this.points = [];
      this.chartWidth = 600;
      return;
    }

    let running = 0;
    const rawPoints = this.history.map((h) => {
      running += h.estimatedRepairCost || 0;
      return { date: h.inspectionDate, status: h.status, defectType: h.defectType, cost: h.estimatedRepairCost || 0, cumulativeCost: running };
    });

    this.maxCumulativeCost = Math.max(...rawPoints.map(p => p.cumulativeCost), 1);

    this.chartWidth = Math.max(600, CHART_LEFT_PADDING + rawPoints.length * CHART_WIDTH_PER_POINT + 40);

    this.points = rawPoints.map((p, i) => {
      const x = CHART_LEFT_PADDING + i * CHART_WIDTH_PER_POINT;
      const statusY = p.status === 'DEFECT' ? STATUS_ROW_Y + 20 : STATUS_ROW_Y;
      const costRatio = p.cumulativeCost / this.maxCumulativeCost;
      const costY = COST_CHART_BOTTOM - costRatio * (COST_CHART_BOTTOM - COST_CHART_TOP);
      return { x, statusY, costY, date: p.date, status: p.status, defectType: p.defectType, cost: p.cost, cumulativeCost: p.cumulativeCost };
    });

    this.statusPolyline = this.points.map(p => `${p.x},${p.statusY}`).join(' ');
    this.costPolyline = this.points.map(p => `${p.x},${p.costY}`).join(' ');
  }
}
