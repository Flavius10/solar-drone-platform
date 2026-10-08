import { Component, OnInit, OnDestroy, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { InspectionService } from '../../services/inspection.service';
import { PanelService } from '../../services/panel.service';
import { Panel } from '../../models/panel.model';
import { AuthService } from '../../services/auth.service';
import { FarmContextService } from '../../services/farm-context.service';
import { TranslatePipe } from '../../pipes/translate.pipe';

@Component({
  selector: 'app-new-inspection',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe],
  templateUrl: './new-inspection.html',
  styleUrl: './new-inspection.scss'
})
export class NewInspection implements OnInit, OnDestroy {
  selectedFile: File | null = null;
  isUploading = false;
  panels: Panel[] = [];
  selectedPanelId: number | null = null;

  selectedInspectionType = 'RGB';

  private farmSub?: Subscription;

  get canUseThermal(): boolean {
    const tier = this.authService.getSubscriptionTier();
    return tier === 'PRO' || tier === 'ENTERPRISE';
  }

  constructor(
    private inspectionService: InspectionService,
    private panelService: PanelService,
    private ngZone: NgZone,
    private cdr: ChangeDetectorRef,
    private authService: AuthService,
    private farmContext: FarmContextService
  ) {}

  ngOnInit(): void {
    this.farmSub = this.farmContext.selectedFarmId$.subscribe(farmId => {
      this.selectedPanelId = null;
      this.loadPanels(farmId);
    });
  }

  ngOnDestroy(): void {
    this.farmSub?.unsubscribe();
  }

  loadPanels(farmId: number | null): void {
    this.panelService.getGrid(farmId).subscribe({
      next: (data) => {
        this.ngZone.run(() => {
          this.panels = data.map(p => ({ id: p.panelId, rowNumber: p.rowNumber, columnNumber: p.columnNumber, label: p.label }));
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          console.error('Failed to load panels', err);
          this.cdr.detectChanges();
        });
      }
    });
  }

  onFileSelected(event: any) {
    const file: File = event.target.files[0];
    if (file) {
      this.selectedFile = file;
    }
  }

  onScan() {
    if (!this.selectedFile) {
      alert('Please select an image before scanning!');
      return;
    }

    if (!this.selectedPanelId) {
      alert('Please select a panel before scanning!');
      return;
    }

    this.isUploading = true;

    this.inspectionService.uploadImage(this.selectedFile, this.selectedPanelId, this.selectedInspectionType).subscribe({
      next: () => {
        this.ngZone.run(() => {
          alert('Imagine incarcata. Se analizeaza in fundal - rezultatul apare in Reports History si pe harta fermei cand e gata.');
          this.isUploading = false;
          this.selectedFile = null;
          this.cdr.detectChanges();
        });
      },
      error: (error) => {
        this.ngZone.run(() => {
          console.error('Upload failed:', error);
          alert('Upload failed. Is the Java server running?');
          this.isUploading = false;
          this.cdr.detectChanges();
        });
      }
    });
  }
}
