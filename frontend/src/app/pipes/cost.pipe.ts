import { Pipe, PipeTransform } from '@angular/core';

const CURRENCY_SYMBOLS: Record<string, string> = { USD: '$', EUR: '€', RON: 'RON' };





@Pipe({ name: 'cost', standalone: true })
export class CostPipe implements PipeTransform {
  transform(amount: number | null | undefined, currencyCode?: string | null): string {
    if (amount === null || amount === undefined) return '-';
    const code = (currencyCode || 'USD').toUpperCase();
    const symbol = CURRENCY_SYMBOLS[code] ?? code;
    const formatted = amount.toFixed(2);
    return code === 'RON' ? `${formatted} ${symbol}` : `${symbol}${formatted}`;
  }
}
