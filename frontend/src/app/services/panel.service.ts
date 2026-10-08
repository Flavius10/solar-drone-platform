import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Panel, PanelGridStatus, BulkImportResult } from '../models/panel.model';
import { InspectionReport } from '../models/inspection.model';

@Injectable({
  providedIn: 'root'
})
export class PanelService {
  private baseUrl = 'http://localhost:8080/api/panels';

  constructor(private http: HttpClient) {}

  getGrid(farmId?: number | null): Observable<PanelGridStatus[]> {
    let params = new HttpParams();
    if (farmId) params = params.set('farmId', farmId);
    return this.http.get<PanelGridStatus[]>(`${this.baseUrl}/grid`, { params });
  }

  createPanel(panel: Panel): Observable<Panel> {
    return this.http.post<Panel>(this.baseUrl, panel);
  }

  updateSpecs(panelId: number, specs: { wattage?: number | null; tier?: string | null }): Observable<Panel> {
    return this.http.patch<Panel>(`${this.baseUrl}/${panelId}`, specs);
  }

  getHistory(panelId: number): Observable<InspectionReport[]> {
    return this.http.get<InspectionReport[]>(`${this.baseUrl}/${panelId}/history`);
  }

  bulkImport(file: File, farmId: number): Observable<BulkImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('farmId', farmId.toString());
    return this.http.post<BulkImportResult>(`${this.baseUrl}/bulk-import`, formData);
  }
}