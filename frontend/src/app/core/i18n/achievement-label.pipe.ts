import { Pipe, PipeTransform, inject } from '@angular/core';
import { TranslateService } from './translate.service';

type AchievementLike = {
  key: string;
  label: string;
  description?: string;
};

/**
 * Resolves an achievement catalog label for the active locale.
 * Key path: {@code achievements.{key}.label}; falls back to API English.
 */
@Pipe({ name: 'achievementLabel', standalone: true, pure: false })
export class AchievementLabelPipe implements PipeTransform {
  private readonly i18n = inject(TranslateService);

  transform(achievement: AchievementLike | null | undefined): string {
    if (!achievement?.key) {
      return achievement?.label ?? '';
    }
    return this.i18n.catalogEntry(
      'achievements',
      achievement.key,
      'label',
      achievement.label
    );
  }
}

/**
 * Resolves an achievement catalog description for the active locale.
 * Key path: {@code achievements.{key}.description}; falls back to API English.
 */
@Pipe({ name: 'achievementDesc', standalone: true, pure: false })
export class AchievementDescPipe implements PipeTransform {
  private readonly i18n = inject(TranslateService);

  transform(achievement: AchievementLike | null | undefined): string {
    if (!achievement?.key) {
      return achievement?.description ?? '';
    }
    return this.i18n.catalogEntry(
      'achievements',
      achievement.key,
      'description',
      achievement.description ?? ''
    );
  }
}
