import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuditLogEntry } from '../models/audit-log.model';
import { PageResponse } from '../models/audit-log.model';

@Injectable({
  providedIn: 'root'
})
export class AuditLogService {
  private baseUrl = 'http://localhost:8080/api/audit-log';

  constructor(private http: HttpClient) {}

  getAll(page: number, size: number): Observable<PageResponse<AuditLogEntry>> {
    return this.http.get<PageResponse<AuditLogEntry>>(`${this.baseUrl}?page=${page}&size=${size}`);
  }
}