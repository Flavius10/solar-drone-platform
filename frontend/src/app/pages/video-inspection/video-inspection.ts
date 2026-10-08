import { Component, OnInit, OnDestroy, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ClarityModule } from '@clr/angular';
import { Subscription } from 'rxjs';
import { InspectionService } from '../../services/inspection.service';
import { FarmContextService } from '../../services/farm-context.service';
import { LiveEventsService } from '../../services/live-events.service';
import { AuthService } from '../../services/auth.service';
import { InspectionReport, VideoInspectionBatch } from '../../models/inspection.model';
import { CostPipe } from '../../pipes/cost.pipe';
import { TranslatePipe } from '../../pipes/translate.pipe';

@Component({
  selector: 'app-video-inspection',
  standalone: true,
  imports: [CommonModule, FormsModule, ClarityModule, CostPipe, TranslatePipe],
  templateUrl: './video-inspection.html',
  styleUrl: './video-inspection.scss'
})
export class VideoInspection implements OnInit, OnDestroy {
  currentFarmId: number | null = null;
  currentFarmName: string | null = null;
  selectedInspectionType = 'RGB';
  selectedFile: File | null = null;

  batches: VideoInspectionBatch[] = [];
  isLoading = false;
  isUploading = false;
  message = '';
  isError = false;

  expandedBatchId: number | null = null;
  framesByBatchId: { [batchId: number]: InspectionReport[] } = {};

  private farmSub?: Subscription;
  private liveSub?: Subscription;

  get canUseThermal(): boolean {
    const tier = this.authService.getSubscriptionTier();
    return tier === 'PRO' || tier === 'ENTERPRISE';
  }

  constructor(
    private inspectionService: InspectionService,
    private farmContext: FarmContextService,
    private liveEvents: LiveEventsService,
    public authService: AuthService,
    private ngZone: NgZone,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.farmSub = this.farmContext.selectedFarmId$.subscribe(farmId => {
      this.currentFarmId = farmId;
      this.currentFarmName = this.farmContext.getFarms().find(f => f.id === farmId)?.name ?? null;
      this.loadBatches();
    });

    this.liveSub = this.liveEvents.events$.subscribe(event => {
      if (event.type.startsWith('VIDEO_BATCH_')) {
        this.applyBatchUpdate(event.data as VideoInspectionBatch);
      }
    });
  }

  ngOnDestroy(): void {
    this.farmSub?.unsubscribe();
    this.liveSub?.unsubscribe();
  }

  loadBatches(): void {
    this.isLoading = true;
    this.inspectionService.getVideoBatches(this.currentFarmId).subscribe({
      next: (data) => {
        this.ngZone.run(() => {
          this.batches = data;
          this.isLoading = false;
          this.cdr.detectChanges();
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
        });
      }
    });
  }

  private applyBatchUpdate(updated: VideoInspectionBatch): void {
    
    
    if (this.currentFarmId && updated.farmId !== this.currentFarmId) return;

    const idx = this.batches.findIndex(b => b.id === updated.id);
    if (idx >= 0) {
      this.batches[idx] = updated;
    } else {
      this.batches = [updated, ...this.batches];
    }
    if (updated.status === 'COMPLETE' && this.expandedBatchId === updated.id) {
      this.loadFrames(updated.id);
    }
    this.cdr.detectChanges();
  }

  onFileSelected(event: any): void {
    const file: File = event.target.files[0];
    if (file) {
      this.selectedFile = file;
    }
  }

  upload(): void {
    if (!this.selectedFile) {
      this.message = 'Selecteaza un fisier video.';
      this.isError = true;
      return;
    }
    if (!this.currentFarmId) {
      this.message = 'Selecteaza o ferma din antet.';
      this.isError = true;
      return;
    }

    this.isUploading = true;
    this.inspectionService.uploadVideo(this.selectedFile, this.currentFarmId, this.selectedInspectionType).subscribe({
      next: (batch) => {
        this.ngZone.run(() => {
          this.message = 'Video incarcat. Extragerea cadrelor si analiza ruleaza in fundal.';
          this.isError = false;
          this.isUploading = false;
          this.selectedFile = null;
          this.batches = [batch, ...this.batches];
          this.cdr.detectChanges();
        });
      },
      error: (error) => {
        this.ngZone.run(() => {
          this.isUploading = false;
          this.message = typeof error.error === 'string' ? error.error : 'Incarcarea videoului a esuat.';
          this.isError = true;
          this.cdr.detectChanges();
        });
      }
    });
  }

  toggleFrames(batch: VideoInspectionBatch): void {
    if (this.expandedBatchId === batch.id) {
      this.expandedBatchId = null;
      return;
    }
    this.expandedBatchId = batch.id;
    if (!this.framesByBatchId[batch.id]) {
      this.loadFrames(batch.id);
    }
  }

  loadFrames(batchId: number): void {
    this.inspectionService.getVideoBatchFrames(batchId).subscribe({
      next: (frames) => {
        this.ngZone.run(() => {
          this.framesByBatchId[batchId] = frames;
          this.cdr.detectChanges();
        });
      },
      error: () => {}
    });
  }

  progressPercent(batch: VideoInspectionBatch): number {
    if (!batch.totalFrames) return 0;
    return Math.round(((batch.processedFrames || 0) / batch.totalFrames) * 100);
  }
}
