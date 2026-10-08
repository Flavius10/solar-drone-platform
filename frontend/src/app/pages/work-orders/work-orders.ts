import { Component, OnInit, OnDestroy, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ClarityModule } from '@clr/angular';
import { Subscription } from 'rxjs';
import { WorkOrderService } from '../../services/work-order.service';
import { AuthService } from '../../services/auth.service';
import { PanelService } from '../../services/panel.service';
import { FarmContextService } from '../../services/farm-context.service';
import { WorkOrder, WorkOrderStatus } from '../../models/work-order.model';
import { Panel } from '../../models/panel.model';
import { TranslatePipe } from '../../pipes/translate.pipe';

@Component({
  selector: 'app-work-orders',
  standalone: true,
  imports: [CommonModule, FormsModule, ClarityModule, TranslatePipe],
  templateUrl: './work-orders.html',
  styleUrl: './work-orders.scss'
})
export class WorkOrders implements OnInit, OnDestroy {
  workOrders: WorkOrder[] = [];
  statuses: WorkOrderStatus[] = ['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

  panels: Panel[] = [];
  currentFarmId: number | null = null;
  newPanelId: number | null = null;
  newTitle = '';
  newDescription = '';

  assigneeByOrderId: { [id: number]: string } = {};

  isLoading = false;
  message = '';
  isError = false;

  
  
  
  
  searchQuery = '';
  statusFilter: WorkOrderStatus | 'ALL' = 'ALL';
  onlyAssignedToMe = false;

  private farmSub?: Subscription;

  constructor(
    private workOrderService: WorkOrderService,
    public authService: AuthService,
    private panelService: PanelService,
    private farmContext: FarmContextService,
    private route: ActivatedRoute,
    private ngZone: NgZone,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const panelIdParam = this.route.snapshot.queryParamMap.get('panelId');

    this.farmSub = this.farmContext.selectedFarmId$.subscribe(farmId => {
      this.currentFarmId = farmId;
      this.newPanelId = null;
      this.loadPanels(farmId, panelIdParam ? Number(panelIdParam) : null);
    });

    this.loadWorkOrders();
  }

  ngOnDestroy(): void {
    this.farmSub?.unsubscribe();
  }

  loadPanels(farmId: number | null, presetPanelId: number | null): void {
    this.panelService.getGrid(farmId).subscribe({
      next: (data) => {
        this.ngZone.run(() => {
          this.panels = data.map(p => ({ id: p.panelId, rowNumber: p.rowNumber, columnNumber: p.columnNumber, label: p.label }));
          if (presetPanelId && this.panels.some(p => p.id === presetPanelId)) {
            this.newPanelId = presetPanelId;
          }
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

  canManage(): boolean {
    const role = this.authService.getRole();
    return role === 'ADMIN' || role === 'TECHNICIAN';
  }

  
  
  toggleOnlyAssignedToMe(): void {
    this.onlyAssignedToMe = !this.onlyAssignedToMe;
  }

  get filteredWorkOrders(): WorkOrder[] {
    const username = (this.authService.getUsername() || '').toLowerCase();
    const query = this.searchQuery.trim().toLowerCase();

    return this.workOrders.filter(wo => {
      if (this.onlyAssignedToMe && (wo.assignedTo || '').toLowerCase() !== username) {
        return false;
      }
      if (this.statusFilter !== 'ALL' && wo.status !== this.statusFilter) {
        return false;
      }
      if (query) {
        const haystack = [
          wo.farmName, `(${wo.panelRow}, ${wo.panelColumn})`, wo.title,
          wo.createdBy, wo.assignedTo
        ].filter(Boolean).join(' ').toLowerCase();
        if (!haystack.includes(query)) return false;
      }
      return true;
    });
  }

  clearFilters(): void {
    this.searchQuery = '';
    this.statusFilter = 'ALL';
    this.onlyAssignedToMe = false;
  }

  loadWorkOrders(): void {
    this.isLoading = true;
    this.workOrderService.getAll().subscribe({
      next: (data) => {
        this.ngZone.run(() => {
          this.workOrders = data;
          this.isLoading = false;
          this.cdr.detectChanges();
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.isLoading = false;
          this.message = 'Failed to load work orders.';
          this.isError = true;
          this.cdr.detectChanges();
        });
      }
    });
  }

  createWorkOrder(): void {
    if (!this.newPanelId || !this.newTitle) {
      this.message = 'Panel ID and title are required.';
      this.isError = true;
      return;
    }

    this.workOrderService.create({
      panelId: this.newPanelId,
      title: this.newTitle,
      description: this.newDescription || undefined
    }).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.message = 'Work order created.';
          this.isError = false;
          this.newPanelId = null;
          this.newTitle = '';
          this.newDescription = '';
          this.loadWorkOrders();
          this.cdr.detectChanges();
        });
      },
      error: (error) => {
        this.ngZone.run(() => {
          this.message = typeof error.error === 'string' ? error.error : 'Failed to create work order.';
          this.isError = true;
          this.cdr.detectChanges();
        });
      }
    });
  }

  assign(workOrder: WorkOrder): void {
    const assignee = this.assigneeByOrderId[workOrder.id];
    if (!assignee) {
      return;
    }
    this.workOrderService.assign(workOrder.id, { assignedTo: assignee }).subscribe({
      next: () => this.ngZone.run(() => { this.loadWorkOrders(); }),
      error: (error) => this.ngZone.run(() => {
        this.message = typeof error.error === 'string' ? error.error : 'Failed to assign work order.';
        this.isError = true;
        this.cdr.detectChanges();
      })
    });
  }

  updateStatus(workOrder: WorkOrder, status: WorkOrderStatus): void {
    this.workOrderService.updateStatus(workOrder.id, { status }).subscribe({
      next: () => this.ngZone.run(() => { this.loadWorkOrders(); }),
      error: (error) => this.ngZone.run(() => {
        this.message = typeof error.error === 'string' ? error.error : 'Failed to update status.';
        this.isError = true;
        this.cdr.detectChanges();
      })
    });
  }
}
