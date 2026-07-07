import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type LootChestVariant = 'iron' | 'sovereign' | 'vault';

@Component({
  selector: 'app-animated-loot-chest',
  standalone: true,
  templateUrl: './animated-loot-chest.component.html',
  styleUrl: './animated-loot-chest.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'animated-loot-chest',
    '[class.animated-loot-chest--sovereign]': 'variant() === "sovereign"',
    '[class.animated-loot-chest--vault]': 'variant() === "vault"',
    '[class.animated-loot-chest--iron]': 'variant() === "iron"',
  },
})
export class AnimatedLootChestComponent {
  readonly variant = input<LootChestVariant>('iron');
}
