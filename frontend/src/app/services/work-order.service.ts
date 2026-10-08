import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { WorkOrder, CreateWorkOrderRequest, AssignWorkOrderRequest, UpdateWorkOrderStatusRequest } from '../models/work-order.model';

@Injectable({
  providedIn: 'root'
})
export class WorkOrderService {
  private baseUrl = 'http://localhost:8080/api/work-orders';

  constructor(private http: HttpClient) {}

  getAll(): Observable<WorkOrder[]> {
    return this.http.get<WorkOrder[]>(this.baseUrl);
  }

  getByPanel(panelId: number): Observable<WorkOrder[]> {
    return this.http.get<WorkOrder[]>(`${this.baseUrl}/panel/${panelId}`);
  }

  create(request: CreateWorkOrderRequest): Observable<WorkOrder> {
    return this.http.post<WorkOrder>(this.baseUrl, request);
  }

  assign(id: number, request: AssignWorkOrderRequest): Observable<WorkOrder> {
    return this.http.put<WorkOrder>(`${this.baseUrl}/${id}/assign`, request);
  }

  updateStatus(id: number, request: UpdateWorkOrderStatusRequest): Observable<WorkOrder> {
    return this.http.put<WorkOrder>(`${this.baseUrl}/${id}/status`, request);
  }
}
