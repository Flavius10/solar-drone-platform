import { Component, signal, OnInit, OnDestroy, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ClarityModule } from '@clr/angular';
import { AuthService } from './services/auth.service';
import { NotificationService } from './services/notification.service';
import { AppNotification } from './models/notification.model';
import { FarmService } from './services/farm.service';
import { FarmContextService } from './services/farm-context.service';
import { Farm } from './models/farm.model';
import { LiveEventsService } from './services/live-events.service';
import { PanelService } from './services/panel.service';
import { PanelGridStatus } from './models/panel.model';
import { Subscription } from 'rxjs';
import { TranslationService, AppLanguage } from './services/translation.service';
import { TranslatePipe } from './pipes/translate.pipe';
import { ThemeService } from './services/theme.service';

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule, RouterOutlet, ClarityModule, RouterLinkActive, RouterLink, TranslatePipe],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit, OnDestroy {
  protected readonly title = signal('frontend');

  notifications: AppNotification[] = [];
  unreadCount = 0;
  showNotifications = false;
  toasts: AppNotification[] = [];

  farms: Farm[] = [];
  selectedFarmId: number | null = null;

  navCollapsed = localStorage.getItem('sidebarCollapsed') === 'true';

  searchQuery = '';
  searchResults: PanelGridStatus[] = [];
  showSearchResults = false;
  searchIsSuggestion = false;
  private farmPanelsCache: PanelGridStatus[] = [];

  private knownIds = new Set<number>();
  private isFirstLoad = true;
  private pollHandle: any;
  private liveEventsSub?: Subscription;

  constructor(
    protected authService: AuthService,
    private notificationService: NotificationService,
    private farmService: FarmService,
    protected farmContext: FarmContextService,
    private liveEvents: LiveEventsService,
    private panelService: PanelService,
    private router: Router,
    private ngZone: NgZone,
    private cdr: ChangeDetectorRef,
    protected translationService: TranslationService,
    protected themeService: ThemeService
  ) {}

  setLanguage(lang: AppLanguage): void {
    this.translationService.setLanguage(lang);
  }

  toggleTheme(): void {
    this.themeService.toggle();
  }

  ngOnInit(): void {
    
    
    this.authService.loggedIn$.subscribe(loggedIn => {
      if (loggedIn) {
        this.isFirstLoad = true;
        this.refreshNotifications();
        this.loadFarms();
        this.liveEvents.connect();
      } else {
        this.notifications = [];
        this.unreadCount = 0;
        this.farms = [];
        this.liveEvents.disconnect();
      }
    });

    this.liveEventsSub = this.liveEvents.events$.subscribe(event => {
      if (event.type === 'notification') {
        this.handleLiveNotification(event.data as AppNotification);
      }
    });

    
    
    this.pollHandle = setInterval(() => {
      if (this.authService.isLoggedIn()) {
        this.refreshNotifications();
      }
    }, 60000);

    this.farmContext.selectedFarmId$.subscribe(id => {
      this.ngZone.run(() => {
        this.selectedFarmId = id;
        this.farmPanelsCache = [];
        this.searchResults = [];
        this.searchQuery = '';
        this.cdr.detectChanges();
      });
    });

    
    
    this.farmContext.farms$.subscribe(farms => {
      this.ngZone.run(() => {
        this.farms = farms;
        this.cdr.detectChanges();
      });
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

  onFarmChange(farmId: string): void {
    this.farmContext.setSelectedFarmId(farmId ? Number(farmId) : null);
  }

  ngOnDestroy(): void {
    if (this.pollHandle) {
      clearInterval(this.pollHandle);
    }
    this.liveEventsSub?.unsubscribe();
    this.liveEvents.disconnect();
  }

  
  
  
  private handleLiveNotification(notification: AppNotification): void {
    if (this.knownIds.has(notification.id)) return;

    this.knownIds.add(notification.id);
    this.notifications = [notification, ...this.notifications];
    this.unreadCount = this.notifications.filter(n => !n.read).length;
    if (!notification.read) {
      this.showToast(notification);
    }
    this.cdr.detectChanges();
  }

  refreshNotifications(): void {
    this.notificationService.getAll().subscribe({
      next: (data) => {
        this.ngZone.run(() => {
          if (!this.isFirstLoad) {
            const newOnes = data.filter(n => !n.read && !this.knownIds.has(n.id));
            newOnes.forEach(n => this.showToast(n));
          }

          this.notifications = data;
          this.unreadCount = data.filter(n => !n.read).length;
          this.knownIds = new Set(data.map(n => n.id));
          this.isFirstLoad = false;
          this.cdr.detectChanges();
        });
      },
      error: () => {}
    });
  }

  showToast(notification: AppNotification): void {
    this.toasts.push(notification);
    this.cdr.detectChanges();
    setTimeout(() => {
      this.ngZone.run(() => {
        this.toasts = this.toasts.filter(t => t.id !== notification.id);
        this.cdr.detectChanges();
      });
    }, 8000);
  }

  dismissToast(notification: AppNotification): void {
    this.toasts = this.toasts.filter(t => t.id !== notification.id);
  }

  toggleNotifications(): void {
    this.showNotifications = !this.showNotifications;
  }

  markAsRead(notification: AppNotification): void {
    if (notification.read) return;
    this.notificationService.markAsRead(notification.id).subscribe({
      next: () => {
        this.ngZone.run(() => {
          notification.read = true;
          this.unreadCount = this.notifications.filter(n => !n.read).length;
          this.cdr.detectChanges();
        });
      },
      error: () => {}
    });
  }

  deleteNotification(notification: AppNotification, event: Event): void {
    event.stopPropagation();
    this.notificationService.delete(notification.id).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.notifications = this.notifications.filter(n => n.id !== notification.id);
          this.knownIds.delete(notification.id);
          this.unreadCount = this.notifications.filter(n => !n.read).length;
          this.cdr.detectChanges();
        });
      },
      error: () => {}
    });
  }

  onLogout(): void {
    this.showNotifications = false;
    this.farmContext.clear();
    this.authService.logout();
  }

  onNavCollapsedChange(collapsed: boolean): void {
    this.navCollapsed = collapsed;
    localStorage.setItem('sidebarCollapsed', String(collapsed));
  }

  
  
  
  onSearchInput(): void {
    const query = this.searchQuery.trim().toLowerCase();
    if (!this.selectedFarmId) {
      this.searchResults = [];
      this.showSearchResults = false;
      return;
    }

    if (!query) {
      this.showSuggestions();
      return;
    }

    this.showSearchResults = true;
    this.searchIsSuggestion = false;

    if (this.farmPanelsCache.length > 0) {
      this.applySearchFilter(query);
      return;
    }

    this.panelService.getGrid(this.selectedFarmId).subscribe({
      next: (panels) => {
        this.ngZone.run(() => {
          this.farmPanelsCache = panels;
          this.applySearchFilter(query);
          this.cdr.detectChanges();
        });
      },
      error: () => {}
    });
  }

  
  
  private showSuggestions(): void {
    if (!this.selectedFarmId) return;
    this.searchIsSuggestion = true;
    this.showSearchResults = true;

    if (this.farmPanelsCache.length > 0) {
      this.applySuggestions();
      return;
    }

    this.panelService.getGrid(this.selectedFarmId).subscribe({
      next: (panels) => {
        this.ngZone.run(() => {
          this.farmPanelsCache = panels;
          this.applySuggestions();
          this.cdr.detectChanges();
        });
      },
      error: () => {}
    });
  }

  private applySuggestions(): void {
    const defects = this.farmPanelsCache.filter(p => p.latestStatus === 'DEFECT');
    const rest = this.farmPanelsCache.filter(p => p.latestStatus !== 'DEFECT');
    this.searchResults = [...defects, ...rest].slice(0, 5);
    this.cdr.detectChanges();
  }

  
  
  
  
  
  private applySearchFilter(query: string): void {
    const nums = (query.match(/\d+/g) || []).map(Number);
    this.searchResults = this.farmPanelsCache.filter(p => {
      const label = (p.label || '').toLowerCase();
      if (label.includes(query)) return true;
      if (nums.length >= 2) return p.rowNumber === nums[0] && p.columnNumber === nums[1];
      if (nums.length === 1) return p.rowNumber === nums[0];
      return false;
    }).slice(0, 8);
    this.cdr.detectChanges();
  }

  goToPanel(panel: PanelGridStatus): void {
    this.showSearchResults = false;
    this.searchQuery = '';
    this.router.navigate(['/farm-map'], { queryParams: { panelId: panel.panelId } });
  }

  onSearchBlur(): void {
    
    setTimeout(() => { this.showSearchResults = false; }, 150);
  }
}
