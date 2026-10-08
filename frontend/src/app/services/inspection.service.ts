import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DashboardStats, InspectionReport, VideoInspectionBatch } from '../models/inspection.model';

@Injectable({
  providedIn: 'root'
})
export class InspectionService {
  private baseUrl = 'http://localhost:8080/api/inspections';

  constructor(private http: HttpClient) {}

  
  
  
  
  uploadImage(file: File, panelId: number, inspectionType: string): Observable<InspectionReport> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('panelId', panelId.toString());
    formData.append('inspectionType', inspectionType);
    return this.http.post<InspectionReport>(`${this.baseUrl}/upload`, formData);
  }

  getById(id: number): Observable<InspectionReport> {
    return this.http.get<InspectionReport>(`${this.baseUrl}/${id}`);
  }

  uploadVideo(file: File, farmId: number, inspectionType: string): Observable<VideoInspectionBatch> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('farmId', farmId.toString());
    formData.append('inspectionType', inspectionType);
    return this.http.post<VideoInspectionBatch>(`${this.baseUrl}/upload-video`, formData);
  }

  getVideoBatches(farmId?: number | null): Observable<VideoInspectionBatch[]> {
    let params = new HttpParams();
    if (farmId) params = params.set('farmId', farmId);
    return this.http.get<VideoInspectionBatch[]>(`${this.baseUrl}/video-batches`, { params });
  }

  getVideoBatch(id: number): Observable<VideoInspectionBatch> {
    return this.http.get<VideoInspectionBatch>(`${this.baseUrl}/video-batches/${id}`);
  }

  getVideoBatchFrames(id: number): Observable<InspectionReport[]> {
    return this.http.get<InspectionReport[]>(`${this.baseUrl}/video-batches/${id}/frames`);
  }

  getDashboardStats(farmId?: number | null): Observable<DashboardStats> {
    let params = new HttpParams();
    if (farmId) params = params.set('farmId', farmId);
    return this.http.get<DashboardStats>(`${this.baseUrl}/stats`, { params });
  }

  getReportsHistory(farmId?: number | null): Observable<InspectionReport[]> {
    let params = new HttpParams();
    if (farmId) params = params.set('farmId', farmId);
    return this.http.get<InspectionReport[]>(`${this.baseUrl}/history`, { params });
  }
}
