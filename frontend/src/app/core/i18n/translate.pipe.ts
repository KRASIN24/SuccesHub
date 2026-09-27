import { Pipe, PipeTransform, inject } from '@angular/core';
import { TranslateService } from './translate.service';

/**
 * Resolves a dotted i18n key against the active translation pack.
 * Pure:false so locale switches update the view without key changes.
 */
@Pipe({ name: 'translate', standalone: true, pure: false })
export class TranslatePipe implements PipeTransform {
  private readonly i18n = inject(TranslateService);

  transform(key: string): string {
    return this.i18n.t(key);
  }
}
