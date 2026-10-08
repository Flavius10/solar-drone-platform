import { Component, OnInit, OnDestroy, NgZone, ChangeDetectorRef} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { PanelService } from '../../services/panel.service';
import { PanelGridStatus, BulkImportResult } from '../../models/panel.model';
import { AuthService } from '../../services/auth.service';
import { FarmContextService } from '../../services/farm-context.service';
import { LiveEventsService } from '../../services/live-events.service';
import { CostPipe } from '../../pipes/cost.pipe';
import { TranslatePipe } from '../../pipes/translate.pipe';

@Component({
  selector: 'app-farm-map',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, CostPipe, TranslatePipe],
  templateUrl: './farm-map.html',
  styleUrl: './farm-map.scss'
})
export class FarmMap implements OnInit, OnDestroy {

  selectedCsvFile: File | null = null;
  isImporting = false;
  importResult: BulkImportResult | null = null;

  panels: PanelGridStatus[] = [];
  selectedPanel: PanelGridStatus | null = null;
  isLoading = true;

  newRow: number | null = null;
  newColumn: number | null = null;
  newLabel = '';
  newWattage: number | null = null;
  newTier: '' | 'BUDGET' | 'STANDARD' | 'PREMIUM' = '';
  isCreating = false;
  createError = '';
  loadError = '';

  isEditingSpecs = false;
  editWattage: number | null = null;
  editTier: '' | 'BUDGET' | 'STANDARD' | 'PREMIUM' = '';
  isSavingSpecs = false;

  currentFarmId: number | null = null;
  private farmSub?: Subscription;
  private liveSub?: Subscription;
  private routeSub?: Subscription;
  private pendingPanelId: number | null = null;

  constructor(
    private panelService: PanelService,
    private ngZone: NgZone,
    private cdr: ChangeDetectorRef,
    private authService: AuthService,
    private farmContext: FarmContextService,
    private liveEvents: LiveEventsService,
    private route: ActivatedRoute
  ) {}

  get isAdmin(): boolean {
    return this.authService.getRole() === 'ADMIN';
  }

  ngOnInit(): void {
    
    
    
    
    this.routeSub = this.route.queryParamMap.subscribe(params => {
      const rawPanelId = params.get('panelId');
      this.pendingPanelId = rawPanelId ? Number(rawPanelId) : null;
      if (this.pendingPanelId != null && this.panels.length > 0) {
        const match = this.panels.find(p => p.panelId === this.pendingPanelId);
        if (match) {
          this.selectedPanel = match;
          this.pendingPanelId = null;
          this.cdr.detectChanges();
        }
      }
    });

    this.farmSub = this.farmContext.selectedFarmId$.subscribe(farmId => {
      this.currentFarmId = farmId;
      this.loadGrid();
    });

    
    
    
    
    this.liveSub = this.liveEvents.events$.subscribe(event => {
      if (event.type === 'INSPECTION_STATUS' || event.type === 'INSPECTION_COMPLETE' || event.type === 'INSPECTION_FAILED') {
        this.refreshGridSilently();
      }
    });
  }

  ngOnDestroy(): void {
    this.farmSub?.unsubscribe();
    this.liveSub?.unsubscribe();
    this.routeSub?.unsubscribe();
  }

  loadGrid(): void {
    this.isLoading = true;
    this.loadError = '';
    this.panelService.getGrid(this.currentFarmId).subscribe({
      next: (data) => {
        this.ngZone.run(() => {
          this.panels = data;
          this.isLoading = false;
          if (this.pendingPanelId != null) {
            const match = this.panels.find(p => p.panelId === this.pendingPanelId);
            if (match) {
              this.selectedPanel = match;
              this.pendingPanelId = null;
            }
          }
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          console.error('Failed to load panel grid', err);
          this.isLoading = false;
          this.loadError = err.status === 403
            ? "You don't have access to this farm."
            : 'Failed to load the farm map.';
          this.cdr.detectChanges();
        });
      }
    });
  }

  
  
  
  private refreshGridSilently(): void {
    this.panelService.getGrid(this.currentFarmId).subscribe({
      next: (data) => {
        this.ngZone.run(() => {
          this.panels = data;
          if (this.selectedPanel) {
            this.selectedPanel = this.panels.find(p => p.panelId === this.selectedPanel!.panelId) ?? this.selectedPanel;
          }
          this.cdr.detectChanges();
        });
      },
      error: () => {}
    });
  }

  get maxRow(): number {
    return this.panels.length ? Math.max(...this.panels.map(p => p.rowNumber)) : 0;
  }

  get maxColumn(): number {
    return this.panels.length ? Math.max(...this.panels.map(p => p.columnNumber)) : 0;
  }

  get rows(): number[] {
    return Array.from({ length: this.maxRow }, (_, i) => i + 1);
  }

  get columns(): number[] {
    return Array.from({ length: this.maxColumn }, (_, i) => i + 1);
  }

  getPanelAt(row: number, column: number): PanelGridStatus | undefined {
    return this.panels.find(p => p.rowNumber === row && p.columnNumber === column);
  }

  getStatusColor(panel: PanelGridStatus | undefined): string {
    if (!panel || panel.latestStatus === 'NO_DATA') return '#6b7280';
    if (panel.latestStatus === 'DEFECT') return '#ef4444';
    if (panel.latestStatus === 'OK') return '#10b981';
    if (panel.latestStatus === 'PROCESSING') return '#f59e0b';
    return '#6b7280';
  }

  selectPanel(panel: PanelGridStatus | undefined): void {
    this.selectedPanel = panel ?? null;
    this.isEditingSpecs = false;
  }

  startEditSpecs(): void {
    if (!this.selectedPanel) return;
    this.editWattage = this.selectedPanel.wattage ?? null;
    this.editTier = this.selectedPanel.tier ?? '';
    this.isEditingSpecs = true;
  }

  cancelEditSpecs(): void {
    this.isEditingSpecs = false;
  }

  saveSpecs(): void {
    if (!this.selectedPanel) return;
    this.isSavingSpecs = true;

    this.panelService.updateSpecs(this.selectedPanel.panelId, {
      wattage: this.editWattage,
      tier: this.editTier || null
    }).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.isSavingSpecs = false;
          this.isEditingSpecs = false;
          this.loadGrid();
          this.cdr.detectChanges();
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.isSavingSpecs = false;
          this.cdr.detectChanges();
        });
      }
    });
  }

  onCreatePanel(): void {
    if (!this.currentFarmId) {
      this.createError = 'Select a farm first.';
      return;
    }
    if (!this.newRow || !this.newColumn) {
      this.createError = 'Row and column are required.';
      return;
    }

    this.isCreating = true;
    this.createError = '';

    this.panelService.createPanel({
      farmId: this.currentFarmId,
      rowNumber: this.newRow,
      columnNumber: this.newColumn,
      label: this.newLabel,
      wattage: this.newWattage ?? undefined,
      tier: this.newTier || undefined
    }).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.isCreating = false;
          this.newRow = null;
          this.newColumn = null;
          this.newLabel = '';
          this.newWattage = null;
          this.newTier = '';
          this.loadGrid();
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          this.isCreating = false;
          this.createError = typeof err.error === 'string' ? err.error : 'Failed to create panel.';
          this.cdr.detectChanges();
        });
      }
    });
  }

  onCsvSelected(event: any): void {
    const file: File = event.target.files[0];
    if (file) {
      this.selectedCsvFile = file;
    }
  }

  onBulkImport(): void {
    if (!this.currentFarmId) {
      alert('Select a farm first!');
      return;
    }
    if (!this.selectedCsvFile) {
      alert('Please select a CSV file first!');
      return;
    }

    this.isImporting = true;
    this.importResult = null;

    this.panelService.bulkImport(this.selectedCsvFile, this.currentFarmId).subscribe({
      next: (result) => {
        this.ngZone.run(() => {
          this.importResult = result;
          this.isImporting = false;
          this.selectedCsvFile = null;
          this.loadGrid();
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          console.error('Bulk import failed', err);
          this.isImporting = false;
          this.cdr.detectChanges();
        });
      }
    });
  }
}
