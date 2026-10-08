import { ChangeDetectorRef, OnDestroy, Pipe, PipeTransform } from '@angular/core';
import { TranslationService } from '../services/translation.service';




@Pipe({ name: 'translate', standalone: true, pure: false })
export class TranslatePipe implements PipeTransform, OnDestroy {
  private unsubscribe: () => void;

  constructor(private translationService: TranslationService, private cdr: ChangeDetectorRef) {
    this.unsubscribe = this.translationService.onChange(() => this.cdr.markForCheck());
  }

  transform(key: string | null | undefined): string {
    if (!key) return '';
    return this.translationService.t(key);
  }

  ngOnDestroy(): void {
    this.unsubscribe();
  }
}
