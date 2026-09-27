import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { RewardItem } from '../../../core/models/gamification.model';
import { LootAudioService } from '../../../core/services/loot-audio.service';
import { RewardLabelPipe } from '../../../core/i18n/reward-label.pipe';
import { TranslateService } from '../../../core/i18n/translate.service';

type CardTier = 'legendary' | 'rare' | 'common';

/**
 * A single loot reward card that starts face-down. Hovering tilts it in 3D
 * (pure-CSS mouse tracker grid) and reveals its rarity colour; clicking flips
 * it to show the reward with a flip sound.
 */
@Component({
  selector: 'app-reveal-card',
  standalone: true,
  imports: [RewardLabelPipe],
  templateUrl: './reveal-card.component.html',
  styleUrl: './reveal-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'loot-card',
    '[class.loot-card--flipped]': 'flipped()',
    '[class.loot-card--legendary]': 'tier() === "legendary"',
    '[class.loot-card--rare]': 'tier() === "rare"',
    '[class.loot-card--common]': 'tier() === "common"',
    role: 'button',
    tabindex: '0',
    '[attr.aria-label]': 'ariaLabel()',
    '(click)': 'flip()',
    '(keydown.enter)': 'flip($event)',
    '(keydown.space)': 'flip($event)',
  },
})
export class RevealCardComponent {
  private readonly audio = inject(LootAudioService);
  private readonly i18n = inject(TranslateService);

  readonly item = input.required<RewardItem>();
  readonly categoryLabel = input<string>('');
  readonly revealed = output<void>();

  readonly flipped = signal(false);
  readonly trackers = Array.from({ length: 25 }, (_, i) => i + 1);

  readonly displayLabel = computed(() => {
    this.i18n.revision();
    const item = this.item();
    return this.i18n.catalogLabel('rewards', item.key, item.label);
  });

  readonly ariaLabel = computed(() =>
    this.flipped() ? this.displayLabel() : 'Hidden reward — activate to reveal'
  );

  readonly tier = computed<CardTier>(() => {
    switch (this.item().rarity) {
      case 'LEGENDARY':
        return 'legendary';
      case 'RARE':
        return 'rare';
      default:
        return 'common';
    }
  });

  flip(event?: Event): void {
    event?.preventDefault();
    if (this.flipped()) return;
    this.flipped.set(true);
    this.audio.playCardFlip();
    const tier = this.tier();
    this.audio.playReveal(tier === 'legendary' ? 'gold' : tier === 'rare' ? 'silver' : 'default');
    this.revealed.emit();
  }
}
