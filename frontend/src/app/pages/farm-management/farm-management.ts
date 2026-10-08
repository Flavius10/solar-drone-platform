import { Component, OnInit, OnDestroy, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ClarityModule } from '@clr/angular';
import { FarmService } from '../../services/farm.service';
import { AuthService } from '../../services/auth.service';
import { FarmContextService } from '../../services/farm-context.service';
import { Farm, DEFECT_TYPES } from '../../models/farm.model';
import { UserSummary } from '../../models/auth.model';
import { TranslatePipe } from '../../pipes/translate.pipe';

@Component({
  selector: 'app-farm-management',
  standalone: true,
  imports: [CommonModule, FormsModule, ClarityModule, TranslatePipe],
  templateUrl: './farm-management.html',
  styleUrl: './farm-management.scss'
})
export class FarmManagement implements OnInit, OnDestroy {
  farms: Farm[] = [];
  newName = '';
  newLocation = '';

  selectedFarmId: number | null = null;
  farmUsers: UserSummary[] = [];
  assignUsername = '';

  defaultsWattage: number | null = null;
  defaultsTier: 'BUDGET' | 'STANDARD' | 'PREMIUM' = 'STANDARD';
  savingDefaults = false;

  currency: 'USD' | 'EUR' | 'RON' = 'USD';
  laborRatePerHour: number | null = null;
  savingPricing = false;

  readonly defectTypes = DEFECT_TYPES;
  criticalThreshold = 0.7;
  criticalTypes = new Set<string>();
  webhookUrl = '';
  savingNotificationSettings = false;
  testingWebhook = false;

  reportCompanyName = '';
  reportAccentColor = '#111c2d';
  hasReportLogo = false;
  logoPreviewUrl: string | null = null;
  selectedLogoFile: File | null = null;
  savingBranding = false;
  uploadingLogo = false;

  isLoading = false;
  message = '';
  isError = false;

  constructor(
    private farmService: FarmService,
    public authService: AuthService,
    private farmContext: FarmContextService,
    private ngZone: NgZone,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadFarms();
  }

  ngOnDestroy(): void {
    this.revokeLogoPreview();
  }

  
  
  
  
  
  private announce(msg: string, isError: boolean): void {
    this.message = msg;
    this.isError = isError;
    setTimeout(() => {
      const scroller = document.querySelector('.content-area');
      if (scroller) {
        scroller.scrollTo({ top: 0, behavior: 'smooth' });
      } else {
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    }, 0);
  }

  private revokeLogoPreview(): void {
    if (this.logoPreviewUrl) {
      URL.revokeObjectURL(this.logoPreviewUrl);
      this.logoPreviewUrl = null;
    }
  }

  private loadLogoPreview(): void {
    this.revokeLogoPreview();
    if (!this.selectedFarmId || !this.hasReportLogo) return;

    this.farmService.getLogoBlob(this.selectedFarmId).subscribe({
      next: (blob) => {
        this.ngZone.run(() => {
          this.logoPreviewUrl = URL.createObjectURL(blob);
          this.cdr.detectChanges();
        });
      },
      error: () => {}
    });
  }

  loadFarms(): void {
    this.farmService.getAll().subscribe({
      next: (data) => {
        this.ngZone.run(() => {
          this.farms = data;
          this.farmContext.setFarms(data);
          this.cdr.detectChanges();
        });
      },
      error: () => {}
    });
  }

  createFarm(): void {
    if (!this.newName) {
      this.announce('Farm name is required.', true);
      return;
    }

    this.farmService.create({ name: this.newName, location: this.newLocation || undefined }).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.announce('Farm created.', false);
          this.newName = '';
          this.newLocation = '';
          this.loadFarms();
          this.cdr.detectChanges();
        });
      },
      error: (error) => {
        this.ngZone.run(() => {
          this.announce(typeof error.error === 'string' ? error.error : 'Failed to create farm.', true);
          this.cdr.detectChanges();
        });
      }
    });
  }

  selectFarm(farm: Farm): void {
    this.selectedFarmId = farm.id;
    this.defaultsWattage = farm.defaultWattage ?? 400;
    this.defaultsTier = farm.defaultTier ?? 'STANDARD';
    this.criticalThreshold = farm.criticalConfidenceThreshold ?? 0.7;
    this.criticalTypes = new Set(farm.criticalDefectTypes ?? ['HOTSPOT', 'CRACK', 'PID_EFFECT', 'DIODE_FAILURE']);
    this.webhookUrl = farm.webhookUrl ?? '';
    this.currency = farm.currency ?? 'USD';
    this.laborRatePerHour = farm.laborRatePerHour ?? null;
    this.reportCompanyName = farm.reportCompanyName ?? '';
    this.reportAccentColor = farm.reportAccentColor ?? '#111c2d';
    this.hasReportLogo = farm.hasReportLogo ?? false;
    this.selectedLogoFile = null;
    this.loadLogoPreview();
    this.farmService.getUsers(farm.id).subscribe({
      next: (data) => {
        this.ngZone.run(() => {
          this.farmUsers = data;
          this.cdr.detectChanges();
        });
      },
      error: () => {}
    });
  }

  assignUser(): void {
    if (!this.selectedFarmId || !this.assignUsername) return;

    this.farmService.assignUser(this.selectedFarmId, this.assignUsername).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.assignUsername = '';
          const farm = this.farms.find(f => f.id === this.selectedFarmId);
          if (farm) this.selectFarm(farm);
          this.cdr.detectChanges();
        });
      },
      error: (error) => {
        this.ngZone.run(() => {
          this.announce(typeof error.error === 'string' ? error.error : 'Failed to assign user.', true);
          this.cdr.detectChanges();
        });
      }
    });
  }

  saveDefaults(): void {
    if (!this.selectedFarmId) return;
    this.savingDefaults = true;

    this.farmService.updateDefaults(this.selectedFarmId, {
      defaultWattage: this.defaultsWattage ?? undefined,
      defaultTier: this.defaultsTier
    }).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.savingDefaults = false;
          this.announce('Farm defaults saved.', false);
          this.loadFarms();
          this.cdr.detectChanges();
        });
      },
      error: (error) => {
        this.ngZone.run(() => {
          this.savingDefaults = false;
          this.announce(typeof error.error === 'string' ? error.error : 'Failed to save farm defaults.', true);
          this.cdr.detectChanges();
        });
      }
    });
  }

  toggleCriticalType(type: string): void {
    if (this.criticalTypes.has(type)) {
      this.criticalTypes.delete(type);
    } else {
      this.criticalTypes.add(type);
    }
  }

  saveNotificationSettings(): void {
    if (!this.selectedFarmId) return;
    this.savingNotificationSettings = true;

    this.farmService.updateNotificationSettings(this.selectedFarmId, {
      criticalConfidenceThreshold: this.criticalThreshold,
      criticalDefectTypes: Array.from(this.criticalTypes),
      webhookUrl: this.webhookUrl
    }).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.savingNotificationSettings = false;
          this.announce('Alert settings saved.', false);
          this.loadFarms();
          this.cdr.detectChanges();
        });
      },
      error: (error) => {
        this.ngZone.run(() => {
          this.savingNotificationSettings = false;
          this.announce(typeof error.error === 'string' ? error.error : 'Failed to save alert settings.', true);
          this.cdr.detectChanges();
        });
      }
    });
  }

  savePricing(): void {
    if (!this.selectedFarmId) return;
    this.savingPricing = true;

    this.farmService.updatePricingSettings(this.selectedFarmId, {
      currency: this.currency,
      laborRatePerHour: this.laborRatePerHour ?? undefined
    }).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.savingPricing = false;
          this.announce('Pricing settings saved.', false);
          this.loadFarms();
          this.cdr.detectChanges();
        });
      },
      error: (error) => {
        this.ngZone.run(() => {
          this.savingPricing = false;
          this.announce(typeof error.error === 'string' ? error.error : 'Failed to save pricing settings.', true);
          this.cdr.detectChanges();
        });
      }
    });
  }

  testWebhook(): void {
    if (!this.selectedFarmId) return;
    this.testingWebhook = true;

    this.farmService.testWebhook(this.selectedFarmId).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.testingWebhook = false;
          this.announce('Test alert sent - check your Slack/webhook channel.', false);
          this.cdr.detectChanges();
        });
      },
      error: (error) => {
        this.ngZone.run(() => {
          this.testingWebhook = false;
          this.announce(typeof error.error === 'string' ? error.error : 'Failed to send test webhook.', true);
          this.cdr.detectChanges();
        });
      }
    });
  }

  saveBranding(): void {
    if (!this.selectedFarmId) return;
    this.savingBranding = true;

    this.farmService.updateBrandingSettings(this.selectedFarmId, {
      reportCompanyName: this.reportCompanyName,
      reportAccentColor: this.reportAccentColor
    }).subscribe({
      next: (farm) => {
        this.ngZone.run(() => {
          this.savingBranding = false;
          this.reportCompanyName = farm.reportCompanyName ?? '';
          this.reportAccentColor = farm.reportAccentColor ?? '#111c2d';
          this.announce('Branding saved.', false);
          this.loadFarms();
          this.cdr.detectChanges();
        });
      },
      error: (error) => {
        this.ngZone.run(() => {
          this.savingBranding = false;
          this.announce(typeof error.error === 'string' ? error.error : 'Failed to save branding.', true);
          this.cdr.detectChanges();
        });
      }
    });
  }

  onLogoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedLogoFile = input.files && input.files.length > 0 ? input.files[0] : null;
  }

  uploadLogo(): void {
    if (!this.selectedFarmId || !this.selectedLogoFile) return;
    this.uploadingLogo = true;

    this.farmService.uploadLogo(this.selectedFarmId, this.selectedLogoFile).subscribe({
      next: (farm) => {
        this.ngZone.run(() => {
          this.uploadingLogo = false;
          this.selectedLogoFile = null;
          this.hasReportLogo = farm.hasReportLogo ?? false;
          this.announce('Logo uploaded.', false);
          this.loadLogoPreview();
          this.loadFarms();
          this.cdr.detectChanges();
        });
      },
      error: (error) => {
        this.ngZone.run(() => {
          this.uploadingLogo = false;
          this.announce(typeof error.error === 'string' ? error.error : 'Failed to upload logo.', true);
          this.cdr.detectChanges();
        });
      }
    });
  }

  removeLogo(): void {
    if (!this.selectedFarmId) return;

    this.farmService.removeLogo(this.selectedFarmId).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.hasReportLogo = false;
          this.revokeLogoPreview();
          this.announce('Logo removed.', false);
          this.loadFarms();
          this.cdr.detectChanges();
        });
      },
      error: (error) => {
        this.ngZone.run(() => {
          this.announce(typeof error.error === 'string' ? error.error : 'Failed to remove logo.', true);
          this.cdr.detectChanges();
        });
      }
    });
  }

  unassignUser(username: string): void {
    if (!this.selectedFarmId) return;

    this.farmService.unassignUser(this.selectedFarmId, username).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.farmUsers = this.farmUsers.filter(u => u.username !== username);
          this.cdr.detectChanges();
        });
      },
      error: () => {}
    });
  }
}
