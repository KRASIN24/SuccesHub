import { Pipe, PipeTransform, inject } from '@angular/core';
import { TranslateService } from './translate.service';

/**
 * Resolves a reward catalog label for the active locale.
 * Key path: {@code rewards.{reward.key}}; falls back to {@code reward.label} (API English).
 */
@Pipe({ name: 'rewardLabel', standalone: true, pure: false })
export class RewardLabelPipe implements PipeTransform {
  private readonly i18n = inject(TranslateService);

  transform(reward: { key: string; label: string } | null | undefined): string {
    if (!reward?.key) {
      return reward?.label ?? '';
    }
    return this.i18n.catalogLabel('rewards', reward.key, reward.label);
  }
}

/**
 * Localized rarity badge (COMMON / RARE / LEGENDARY).
 */
@Pipe({ name: 'rarityLabel', standalone: true, pure: false })
export class RarityLabelPipe implements PipeTransform {
  private readonly i18n = inject(TranslateService);

  transform(rarity: string | null | undefined): string {
    if (!rarity) {
      return '';
    }
    return this.i18n.catalogLabel('rarity', rarity, rarity);
  }
}

/**
 * Localized reward effect blurb.
 * Key path: {@code rewards.effects.{reward.key}}; falls back to API effect / provided default.
 */
@Pipe({ name: 'rewardEffect', standalone: true, pure: false })
export class RewardEffectPipe implements PipeTransform {
  private readonly i18n = inject(TranslateService);

  transform(
    reward: { key: string; effect?: string | null } | null | undefined,
    fallback = ''
  ): string {
    if (!reward?.key) {
      return reward?.effect || fallback;
    }
    return this.i18n.catalogLabel(
      'rewards.effects',
      reward.key,
      reward.effect || fallback
    );
  }
}
