import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ReportService {
  private baseUrl = 'http://localhost:8080/api/reports';

  constructor(private http: HttpClient) {}

  exportExcel(farmId: number | null): Observable<Blob> {
    let params = new HttpParams();
    if (farmId != null) params = params.set('farmId', farmId);
    return this.http.get(`${this.baseUrl}/export/excel`, { params, responseType: 'blob' });
  }

  exportPdf(farmId: number | null): Observable<Blob> {
    let params = new HttpParams();
    if (farmId != null) params = params.set('farmId', farmId);
    return this.http.get(`${this.baseUrl}/export/pdf`, { params, responseType: 'blob' });
  }
}
