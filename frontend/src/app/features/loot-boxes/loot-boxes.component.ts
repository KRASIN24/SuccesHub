import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin, switchMap } from 'rxjs';
import { GamificationService } from '../../core/services/gamification.service';
import { LootAudioService } from '../../core/services/loot-audio.service';
import { BoxType, InventoryItem, LootBox, RewardItem } from '../../core/models/gamification.model';
import { LoadingSkeletonComponent } from '../../shared/components/loading-skeleton/loading-skeleton.component';
import { ErrorStateComponent } from '../../shared/components/error-state/error-state.component';
import {
  AnimatedLootChestComponent,
  LootChestVariant,
} from './animated-loot-chest/animated-loot-chest.component';
import { RevealCardComponent } from './reveal-card/reveal-card.component';

type Phase = 'idle' | 'charging' | 'revealing';
type GlowTier = 'default' | 'silver' | 'gold';
type Rarity = 'COMMON' | 'RARE' | 'LEGENDARY';
type Category = 'UTILITY' | 'TITLE' | 'AVATAR_FRAME';

interface RecentHistoryEntry {
  entryId: string;
  item: RewardItem;
  boxName: string;
  pulledAt: number;
}

interface FooterLink {
  label: string;
}

interface RewardTier {
  name: string;
  description: string[];
  tierLabel: string;
  progress: number;
  accent: string;
  softAccent: string;
  icon: string;
  cardBorder: string;
  cardOverlay: string;
  gradientFrom: string;
  gradientTo: string;
  iconBorder: string;
  glow: string;
  shape: 'circle' | 'rounded';
}

const LOOT_ART: Record<string, string> = {
  ARCANE_ORB: 'loot/glowing_orb.png',
  IRON_CHEST: 'loot/treasure_chest.png',
  SOVEREIGN_VAULT: 'loot/divine_vault.png',
};

const RECENT_HISTORY_KEY = 'succes-hub:loot-recent-history';
const RECENT_HISTORY_MAX = 8;

const TIER_TEMPLATES: Omit<RewardTier, 'name'>[] = [
  {
    description: ['Daily utility and small boosts.'],
    tierLabel: 'Common Tier',
    progress: 100,
    accent: '#d4d4d4',
    softAccent: '#737373',
    icon: LOOT_ART['ARCANE_ORB'],
    cardBorder: 'rgba(255,255,255,0.05)',
    cardOverlay: 'transparent',
    gradientFrom: 'rgba(212,212,212,0.2)',
    gradientTo: 'rgba(115,115,115,0.1)',
    iconBorder: 'rgba(163,163,163,0.3)',
    glow: 'rgba(245,245,245,0.1)',
    shape: 'circle',
  },
  {
    description: ['Milestone loot with better odds.'],
    tierLabel: 'Rare Tier',
    progress: 66.67,
    accent: '#f5be42',
    softAccent: '#f5be42',
    icon: LOOT_ART['IRON_CHEST'],
    cardBorder: 'rgba(245,190,66,0.2)',
    cardOverlay: 'rgba(245,190,66,0.05)',
    gradientFrom: 'rgba(245,190,66,0.2)',
    gradientTo: 'rgba(231,194,104,0.1)',
    iconBorder: 'rgba(245,190,66,0.4)',
    glow: 'rgba(245,190,66,0.2)',
    shape: 'rounded',
  },
  {
    description: ['Top rewards: titles and legendaries.'],
    tierLabel: 'Epic Tier',
    progress: 25,
    accent: '#ffb2b7',
    softAccent: '#ffb2b7',
    icon: LOOT_ART['SOVEREIGN_VAULT'],
    cardBorder: 'rgba(255,178,183,0.3)',
    cardOverlay: 'rgba(255,178,183,0.1)',
    gradientFrom: 'rgba(93,0,24,1)',
    gradientTo: 'rgba(135,35,52,1)',
    iconBorder: 'rgba(255,178,183,0.5)',
    glow: 'rgba(255,178,183,0.4)',
    shape: 'circle',
  },
];

@Component({
  selector: 'app-loot-boxes',
  standalone: true,
  imports: [
    CommonModule,
    LoadingSkeletonComponent,
    ErrorStateComponent,
    AnimatedLootChestComponent,
    RevealCardComponent,
  ],
  templateUrl: './loot-boxes.component.html',
  styleUrl: './loot-boxes.component.scss',
})
export class LootBoxesComponent implements OnInit {
  private readonly gamification = inject(GamificationService);
  private readonly lootAudio = inject(LootAudioService);

  readonly navPrevIcon = 'https://www.figma.com/api/mcp/asset/c32ac360-fb11-43c3-8a87-8987a3ddb081';
  readonly navNextIcon = 'https://www.figma.com/api/mcp/asset/58580b05-b667-4c70-86a2-93d7ba92670c';
  readonly accentIcon = 'https://www.figma.com/api/mcp/asset/af0f7201-80fd-4bcb-9736-a5b8ad19a27d';

  readonly footerLinks: FooterLink[] = [
    { label: 'Manifest History' },
    { label: 'Drop Rates' },
    { label: 'Exchange' },
  ];

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);

  readonly boxTypes = signal<BoxType[]>([]);
  readonly pendingBoxes = signal<LootBox[]>([]);
  readonly inventory = signal<InventoryItem[]>([]);

  readonly selectedIndex = signal(0);

  readonly phase = signal<Phase>('idle');
  readonly activeBox = signal<BoxType | null>(null);
  readonly rolled = signal<RewardItem[]>([]);
  readonly glow = signal<GlowTier>('default');
  readonly boxesOpened = signal(0);
  readonly flippedCount = signal(0);
  readonly allRevealed = computed(
    () => this.rolled().length > 0 && this.flippedCount() >= this.rolled().length
  );

  readonly selectedBox = computed<BoxType | null>(() => this.boxTypes()[this.selectedIndex()] ?? null);
  readonly selectedHeroImageUrl = computed(() => {
    const box = this.selectedBox();
    return box ? this.heroImageForBox(box) : LOOT_ART['IRON_CHEST'];
  });
  readonly chestSrc = computed(() => this.selectedHeroImageUrl());
  readonly activeBoxImageUrl = computed(() => {
    const box = this.activeBox();
    return box ? this.heroImageForBox(box) : '';
  });
  readonly selectedPendingCount = computed(() => {
    const box = this.selectedBox();
    return box ? this.pendingCount(box.id) : 0;
  });

  /** Recent pulls, newest first — persisted in localStorage across visits. */
  readonly recentHistory = signal<RecentHistoryEntry[]>([]);

  readonly primaryLabel = computed(() => 'Open Cache');

  readonly ctaHint = computed(() => {
    const pending = this.selectedPendingCount();
    if (pending > 0) {
      return `${pending} cache${pending === 1 ? '' : 's'} ready to open`;
    }
    return 'Summons a cache and opens instantly';
  });

  readonly canOpen = computed(() => !this.busy() && this.phase() === 'idle' && !!this.selectedBox());

  readonly heroChestClass = computed(() => this.chestClassForBox(this.selectedBox()));

  readonly activeChargingChestClass = computed(() => this.chestClassForBox(this.activeBox()));

  readonly chestVariant = computed<LootChestVariant>(() => {
    const id = this.selectedBox()?.id;
    if (id === 'IRON_CHEST') return 'sovereign';
    if (id === 'SOVEREIGN_VAULT') return 'vault';
    return 'iron';
  });

  get rewardTiers(): RewardTier[] {
    return this.boxTypes().map((box, i) => {
      const template = TIER_TEMPLATES[Math.min(i, TIER_TEMPLATES.length - 1)];
      return { name: box.name, ...template };
    });
  }

  /**
   * Hero artwork per artifact tier.
   */
  heroImageForBox(box: BoxType): string {
    return LOOT_ART[box.id] ?? LOOT_ART['IRON_CHEST'];
  }

  private chestClassForBox(box: BoxType | null): string {
    if (!box) return '';
    switch (box.id) {
      case 'ARCANE_ORB':
        return 'hero__chest--orb';
      case 'IRON_CHEST':
        return 'hero__chest--sovereign';
      case 'SOVEREIGN_VAULT':
        return 'hero__chest--vault';
      default:
        return '';
    }
  }

  onCardRevealed(): void {
    this.flippedCount.update((n) => n + 1);
  }

  private beginOpening(box: BoxType, contents: RewardItem[]): void {
    this.activeBox.set(box);
    this.rolled.set(contents);
    this.flippedCount.set(0);
    this.glow.set(this.glowTierFor(contents));
    this.pushRecentHistory(box, contents);
    this.phase.set('charging');
    this.boxesOpened.update((n) => n + 1);
    this.busy.set(false);
    this.lootAudio.playChestOpen();
    setTimeout(() => {
      this.phase.set('revealing');
      this.lootAudio.playReveal(this.glow());
    }, 950);
  }

  ngOnInit(): void {
    this.recentHistory.set(this.loadRecentHistory());
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

  // --- Carousel ---

  select(index: number): void {
    if (this.phase() !== 'idle') return;
    this.selectedIndex.set(index);
    this.scrollPageToTop();
  }

  /** Scroll the main app shell so the hero hex is back in view after tier pick. */
  private scrollPageToTop(): void {
    if (typeof document === 'undefined') return;
    document.querySelector('.app-main')?.scrollTo({ top: 0, behavior: 'smooth' });
  }

  private pushRecentHistory(box: BoxType, contents: RewardItem[]): void {
    const now = Date.now();
    const entries: RecentHistoryEntry[] = contents.map((item, i) => ({
      entryId: `${now}-${i}-${item.key}`,
      item,
      boxName: box.name,
      pulledAt: now,
    }));
    const next = [...entries, ...this.recentHistory()].slice(0, RECENT_HISTORY_MAX);
    this.recentHistory.set(next);
    this.saveRecentHistory(next);
  }

  private loadRecentHistory(): RecentHistoryEntry[] {
    if (typeof localStorage === 'undefined') return [];
    try {
      const raw = localStorage.getItem(RECENT_HISTORY_KEY);
      if (!raw) return [];
      const parsed = JSON.parse(raw) as RecentHistoryEntry[];
      return Array.isArray(parsed) ? parsed.slice(0, RECENT_HISTORY_MAX) : [];
    } catch {
      return [];
    }
  }

  private saveRecentHistory(entries: RecentHistoryEntry[]): void {
    if (typeof localStorage === 'undefined') return;
    try {
      localStorage.setItem(RECENT_HISTORY_KEY, JSON.stringify(entries));
    } catch {
      // ignore quota / private mode
    }
  }

  formatPulledAt(pulledAt: number): string {
    const diff = Date.now() - pulledAt;
    if (diff < 60_000) return 'Just now';
    if (diff < 3_600_000) return `${Math.floor(diff / 60_000)}m ago`;
    if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)}h ago`;
    return `${Math.floor(diff / 86_400_000)}d ago`;
  }

  next(): void {
    const n = this.boxTypes().length;
    if (!n || this.phase() !== 'idle') return;
    this.selectedIndex.update((i) => (i + 1) % n);
  }

  prev(): void {
    const n = this.boxTypes().length;
    if (!n || this.phase() !== 'idle') return;
    this.selectedIndex.update((i) => (i - 1 + n) % n);
  }

  onPrevChest(): void {
    this.prev();
  }

  onNextChest(): void {
    this.next();
  }

  onOpenCache(): void {
    this.primaryAction();
  }

  /** Opens a held cache, or grants one and opens immediately when none are held. */
  primaryAction(): void {
    const box = this.selectedBox();
    if (!box) return;
    if (this.selectedPendingCount() > 0) {
      this.open(box);
    } else {
      this.grantAndOpen(box);
    }
  }

  /** Grants a pending cache of the selected type WITHOUT opening it (earn-simulation). */
  summon(): void {
    const box = this.selectedBox();
    if (!box || this.busy() || this.phase() !== 'idle') return;
    this.busy.set(true);
    this.gamification.grantLootBox(box.id).subscribe({
      next: (created) => {
        this.pendingBoxes.update((list) => [created, ...list]);
        this.busy.set(false);
      },
      error: () => {
        this.error.set('Could not summon a cache.');
        this.busy.set(false);
      },
    });
  }

  private grantAndOpen(box: BoxType): void {
    if (this.busy() || this.phase() !== 'idle') return;
    this.busy.set(true);
    this.gamification
      .grantLootBox(box.id)
      .pipe(
        switchMap((created) => {
          this.pendingBoxes.update((list) => [created, ...list]);
          return this.gamification.openLootBox(created.id);
        })
      )
      .subscribe({
        next: (opened) => {
          this.beginOpening(box, opened.contents);
        },
        error: () => {
          this.error.set('Could not open the cache.');
          this.busy.set(false);
        },
      });
  }

  pendingCount(boxTypeId: string): number {
    return this.pendingBoxes().filter((b) => b.boxType === boxTypeId).length;
  }

  pendingCountAt(index: number): number {
    const box = this.boxTypes()[index];
    return box ? this.pendingCount(box.id) : 0;
  }

  open(box: BoxType): void {
    if (this.busy() || this.phase() !== 'idle') return;
    const pending = this.pendingBoxes().find((b) => b.boxType === box.id);
    if (!pending) return;

    this.busy.set(true);
    this.gamification.openLootBox(pending.id).subscribe({
      next: (opened) => {
        this.beginOpening(box, opened.contents);
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
    this.flippedCount.set(0);
    this.glow.set('default');
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

  /** Rarity modifier class for chips/thumbnails (recent drops preview). */
  rarityClass(rarity: string): string {
    switch (rarity) {
      case 'LEGENDARY':
        return 'is-legendary';
      case 'RARE':
        return 'is-rare';
      default:
        return 'is-common';
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

  private glowTierFor(items: RewardItem[]): GlowTier {
    if (items.some((i) => i.rarity === 'LEGENDARY')) return 'gold';
    if (items.some((i) => i.rarity === 'RARE')) return 'silver';
    return 'default';
  }

  // --- Reveal overlay styling ---

  revealEyebrowClass(): string {
    switch (this.glowRarity()) {
      case 'LEGENDARY':
        return 'reveal__eyebrow--legendary';
      case 'RARE':
        return 'reveal__eyebrow--rare';
      default:
        return 'reveal__eyebrow--common';
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
