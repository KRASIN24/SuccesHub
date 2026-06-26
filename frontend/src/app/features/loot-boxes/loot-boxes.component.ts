import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';
import { GamificationService } from '../../core/services/gamification.service';
import { BoxType, InventoryItem, LootBox, RewardItem } from '../../core/models/gamification.model';
import { LoadingSkeletonComponent } from '../../shared/components/loading-skeleton/loading-skeleton.component';
import { ErrorStateComponent } from '../../shared/components/error-state/error-state.component';

type Phase = 'idle' | 'charging' | 'revealing';
type GlowTier = 'default' | 'silver' | 'gold';
type Rarity = 'COMMON' | 'RARE' | 'LEGENDARY';
type Category = 'UTILITY' | 'TITLE' | 'AVATAR_FRAME';

@Component({
  selector: 'app-loot-boxes',
  standalone: true,
  imports: [CommonModule, LoadingSkeletonComponent, ErrorStateComponent],
  templateUrl: './loot-boxes.component.html',
  styleUrl: './loot-boxes.component.scss',
})
export class LootBoxesComponent implements OnInit {
  private readonly gamification = inject(GamificationService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);

  readonly boxTypes = signal<BoxType[]>([]);
  readonly pendingBoxes = signal<LootBox[]>([]);
  readonly inventory = signal<InventoryItem[]>([]);

  readonly phase = signal<Phase>('idle');
  readonly activeBox = signal<BoxType | null>(null);
  readonly rolled = signal<RewardItem[]>([]);
  readonly glow = signal<GlowTier>('default');
  readonly boxesOpened = signal(0);

  readonly totalItems = computed(() => this.inventory().reduce((sum, i) => sum + i.quantity, 0));
  readonly utilityStacks = computed(() => this.byCategory('UTILITY'));
  readonly titleStacks = computed(() => this.byCategory('TITLE'));
  readonly frameStacks = computed(() => this.byCategory('AVATAR_FRAME'));

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      types: this.gamification.getBoxTypes(),
      pending: this.gamification.getPendingLootBoxes(),
      inventory: this.gamification.getInventory(),
    }).subscribe({
      next: ({ types, pending, inventory }) => {
        this.boxTypes.set(types);
        this.pendingBoxes.set(pending);
        this.inventory.set(inventory);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not reach the vault. Try again.');
        this.loading.set(false);
      },
    });
  }

  pendingCount(boxTypeId: string): number {
    return this.pendingBoxes().filter((b) => b.boxType === boxTypeId).length;
  }

  /** Summon (grant) a pending box of a type — earn-simulation entry point. */
  summon(box: BoxType): void {
    if (this.busy() || this.phase() !== 'idle') return;
    this.busy.set(true);
    this.gamification.grantLootBox(box.id).subscribe({
      next: (created) => {
        this.pendingBoxes.update((list) => [created, ...list]);
        this.busy.set(false);
      },
      error: () => {
        this.error.set('Could not summon a box.');
        this.busy.set(false);
      },
    });
  }

  open(box: BoxType): void {
    if (this.busy() || this.phase() !== 'idle') return;
    const pending = this.pendingBoxes().find((b) => b.boxType === box.id);
    if (!pending) return;

    this.busy.set(true);
    this.gamification.openLootBox(pending.id).subscribe({
      next: (opened) => {
        this.activeBox.set(box);
        this.rolled.set(opened.contents);
        this.glow.set(this.glowTierFor(opened.contents));
        this.phase.set('charging');
        this.boxesOpened.update((n) => n + 1);
        this.busy.set(false);
        setTimeout(() => this.phase.set('revealing'), 950);
      },
      error: () => {
        this.error.set('Could not open the box.');
        this.busy.set(false);
      },
    });
  }

  collect(): void {
    this.phase.set('idle');
    this.activeBox.set(null);
    this.rolled.set([]);
    this.glow.set('default');
    // Reconcile with server truth (opened box is no longer pending; inventory grew).
    forkJoin({
      pending: this.gamification.getPendingLootBoxes(),
      inventory: this.gamification.getInventory(),
    }).subscribe({
      next: ({ pending, inventory }) => {
        this.pendingBoxes.set(pending);
        this.inventory.set(inventory);
      },
    });
  }

  equip(item: InventoryItem): void {
    if (this.categoryOf(item.reward) === 'UTILITY' || this.busy()) return;
    this.busy.set(true);
    this.gamification.equipInventoryItem(item.id).subscribe({
      next: () => {
        this.gamification.getInventory().subscribe({
          next: (inv) => {
            this.inventory.set(inv);
            this.busy.set(false);
          },
          error: () => this.busy.set(false),
        });
      },
      error: () => this.busy.set(false),
    });
  }

  categoryOf(reward: RewardItem): Category {
    switch (reward.type) {
      case 'TITLE':
        return 'TITLE';
      case 'FRAME':
        return 'AVATAR_FRAME';
      default:
        return 'UTILITY';
    }
  }

  categoryLabel(reward: RewardItem): string {
    switch (this.categoryOf(reward)) {
      case 'TITLE':
        return 'Title';
      case 'AVATAR_FRAME':
        return 'Avatar Frame';
      default:
        return 'Utility';
    }
  }

  weightPct(box: BoxType, rarity: Rarity): number {
    switch (rarity) {
      case 'RARE':
        return box.rareWeight;
      case 'LEGENDARY':
        return box.legendaryWeight;
      default:
        return box.commonWeight;
    }
  }

  private byCategory(category: Category): InventoryItem[] {
    return this.inventory().filter((i) => this.categoryOf(i.reward) === category);
  }

  private glowTierFor(items: RewardItem[]): GlowTier {
    if (items.some((i) => i.rarity === 'LEGENDARY')) return 'gold';
    if (items.some((i) => i.rarity === 'RARE')) return 'silver';
    return 'default';
  }

  // --- Presentation helpers (literal class strings keep Tailwind JIT happy) ---

  rarityLabelClass(rarity: string): string {
    switch (rarity) {
      case 'LEGENDARY':
        return 'text-primary';
      case 'RARE':
        return 'text-slate-200';
      default:
        return 'text-on-surface-variant';
    }
  }

  rarityCardClass(rarity: string): string {
    switch (rarity) {
      case 'LEGENDARY':
        return 'border-primary/60 shadow-[0_0_35px_rgba(245,190,66,0.45)]';
      case 'RARE':
        return 'border-slate-300/40 shadow-[0_0_25px_rgba(203,213,225,0.25)]';
      default:
        return 'border-outline-variant/30';
    }
  }

  glowClass(): string {
    switch (this.glow()) {
      case 'gold':
        return 'glow-gold';
      case 'silver':
        return 'glow-silver';
      default:
        return 'glow-default';
    }
  }

  glowHeadline(): string {
    switch (this.glow()) {
      case 'gold':
        return 'Legendary pull';
      case 'silver':
        return 'Rare find';
      default:
        return 'Your haul';
    }
  }

  glowRarity(): Rarity {
    switch (this.glow()) {
      case 'gold':
        return 'LEGENDARY';
      case 'silver':
        return 'RARE';
      default:
        return 'COMMON';
    }
  }
}
