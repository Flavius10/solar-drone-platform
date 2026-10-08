import { Injectable, NgZone } from '@angular/core';
import { Subject } from 'rxjs';
import { AuthService } from './auth.service';

export interface LiveEvent<T = any> {
  type: string;
  data: T;
}

const STREAM_URL = 'http://localhost:8080/api/notifications/stream';

const EVENT_NAMES = [
  'connected',
  'notification',
  'INSPECTION_STATUS',
  'INSPECTION_COMPLETE',
  'INSPECTION_FAILED',
  'VIDEO_BATCH_STATUS',
  'VIDEO_BATCH_PROGRESS',
  'VIDEO_BATCH_COMPLETE',
  'VIDEO_BATCH_FAILED'
];


@Injectable({
  providedIn: 'root'
})
export class LiveEventsService {
  private eventSource: EventSource | null = null;
  private eventsSubject = new Subject<LiveEvent>();
  events$ = this.eventsSubject.asObservable();

  constructor(private authService: AuthService, private ngZone: NgZone) {}

  connect(): void {
    if (this.eventSource) return;
    const token = this.authService.getToken();
    if (!token) return;

    this.eventSource = new EventSource(`${STREAM_URL}?token=${encodeURIComponent(token)}`);

    for (const name of EVENT_NAMES) {
      this.eventSource.addEventListener(name, (event: MessageEvent) => {
        this.ngZone.run(() => {
          let data: any = event.data;
          try {
            data = JSON.parse(event.data);
          } catch {
            
          }
          this.eventsSubject.next({ type: name, data });
        });
      });
    }
  }

  disconnect(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }
}
