import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Farm } from '../models/farm.model';

const SELECTED_FARM_KEY = 'selected_farm_id';

@Injectable({
  providedIn: 'root'
})
export class FarmContextService {
  private selectedFarmIdSubject = new BehaviorSubject<number | null>(this.readStoredFarmId());
  selectedFarmId$ = this.selectedFarmIdSubject.asObservable();

  private farmsSubject = new BehaviorSubject<Farm[]>([]);
  farms$ = this.farmsSubject.asObservable();

  private readStoredFarmId(): number | null {
    const stored = localStorage.getItem(SELECTED_FARM_KEY);
    return stored ? Number(stored) : null;
  }

  setFarms(farms: Farm[]): void {
    this.farmsSubject.next(farms);

    const current = this.selectedFarmIdSubject.value;
    const stillValid = farms.some(f => f.id === current);
    if (!stillValid && farms.length > 0) {
      this.setSelectedFarmId(farms[0].id);
    }
  }

  getSelectedFarmId(): number | null {
    return this.selectedFarmIdSubject.value;
  }

  setSelectedFarmId(farmId: number | null): void {
    this.selectedFarmIdSubject.next(farmId);
    if (farmId !== null) {
      localStorage.setItem(SELECTED_FARM_KEY, String(farmId));
    } else {
      localStorage.removeItem(SELECTED_FARM_KEY);
    }
  }

  getFarms(): Farm[] {
    return this.farmsSubject.value;
  }

  clear(): void {
    this.farmsSubject.next([]);
    this.selectedFarmIdSubject.next(null);
    localStorage.removeItem(SELECTED_FARM_KEY);
  }
}
