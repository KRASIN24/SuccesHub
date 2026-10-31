import { CommonModule } from '@angular/common';
import {
  Component,
  ElementRef,
  HostListener,
  Injector,
  OnDestroy,
  OnInit,
  afterNextRender,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { DevToolId, DevToolsService } from '../../../core/services/dev-tools.service';
import { GamificationService } from '../../../core/services/gamification.service';
import { LootPendingService } from '../../../core/services/loot-pending.service';

const DOCK_POS_KEY = 'successhub_dev_dock_fab';
const DOCK_MARGIN_PX = 20;
const DRAG_THRESHOLD_PX = 6;

const TOOL_META: Record<DevToolId, { label: string; icon: string; title: string }> = {
  streak: { label: 'Streak', icon: 'local_fire_department', title: 'Streak tools' },
  cache: { label: 'Cache', icon: 'inventory_2', title: 'Cache tools' },
  time: { label: 'Time', icon: 'schedule', title: 'Virtual clock' },
  ritual: { label: 'Ritual', icon: 'auto_awesome', title: 'Ritual tools' },
  progress: { label: 'Progress', icon: 'trending_up', title: 'XP and level' },
  inventory: { label: 'Items', icon: 'backpack', title: 'Inventory grants' },
  combat: { label: 'Combat', icon: 'swords', title: 'Shields and armed cards' },
  campaigns: { label: 'Campaigns', icon: 'flag', title: 'Campaign Strike tools' },
};

/**
 * Global floating Dev dock + expanded messenger panel for gated testing tools.
 * FAB is positioned by top-left; the panel is absolutely anchored to it so
 * open/close/flip never moves the bubble.
 */
@Component({
  selector: 'app-dev-tools-host',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './dev-tools-host.component.html',
  styleUrl: './dev-tools-host.component.scss',
  host: {
    '[class.dev-host--dragging]': 'dragging()',
  },
})
export class DevToolsHostComponent implements OnInit, OnDestroy {
  readonly tools = inject(DevToolsService);
  private readonly gamification = inject(GamificationService);
  private readonly lootPending = inject(LootPendingService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly hostEl = inject(ElementRef<HTMLElement>);
  private readonly injector = inject(Injector);

  readonly toolMeta = TOOL_META;
  readonly toolIds: DevToolId[] = [
    'streak',
    'cache',
    'time',
    'ritual',
    'progress',
    'inventory',
    'combat',
    'campaigns',
  ];

  /** FAB top-left in viewport pixels. */
  readonly fabPos = signal<{ left: number; top: number }>(this.roughDefaultFabPos());
  readonly dragging = signal(false);
  /** When true, panel opens under the FAB (not enough room above). */
  readonly panelBelow = signal(false);
  /** When true, panel left-edges with the FAB (not enough room to extend left). */
  readonly panelAlignStart = signal(false);
  /** Caps panel height to available space. */
  readonly panelMaxHeightPx = signal<number | null>(null);

  private dragState: {
    pointerId: number;
    startX: number;
    startY: number;
    originLeft: number;
    originTop: number;
    moved: boolean;
  } | null = null;
  private suppressClick = false;
  private hasCustomPos = false;

  ngOnInit(): void {
    const stored = this.readStoredPos();
    if (stored) {
      this.fabPos.set(stored);
      this.hasCustomPos = true;
    }
    this.tools.refresh();
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd)).subscribe(() => {
      if (this.auth.isLoggedIn()) {
        this.tools.refresh();
      }
    });
    afterNextRender(() => {
      if (!this.hasCustomPos) {
        this.fabPos.set(this.defaultFabPos());
      }
      this.clampFabToViewport();
      this.updatePanelPlacement();
    }, { injector: this.injector });
  }

  ngOnDestroy(): void {
    this.dragState = null;
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    if (!this.hasCustomPos) {
      this.fabPos.set(this.defaultFabPos());
    }
    this.clampFabToViewport();
    this.updatePanelPlacement();
    this.syncPanelMaxHeight();
  }

  toggleDock(): void {
    if (this.suppressClick) {
      this.suppressClick = false;
      return;
    }
    if (this.tools.openTool()) {
      this.tools.close();
      this.panelBelow.set(false);
      this.panelAlignStart.set(false);
      this.panelMaxHeightPx.set(null);
      return;
    }
    this.updatePanelPlacement();
    this.tools.open('progress');
    afterNextRender(() => this.syncPanelMaxHeight(), { injector: this.injector });
  }

  onFabPointerDown(event: PointerEvent): void {
    if (event.button !== 0) {
      return;
    }
    const pos = this.fabPos();
    this.dragState = {
      pointerId: event.pointerId,
      startX: event.clientX,
      startY: event.clientY,
      originLeft: pos.left,
      originTop: pos.top,
      moved: false,
    };
    (event.currentTarget as HTMLElement).setPointerCapture(event.pointerId);
  }

  onFabPointerMove(event: PointerEvent): void {
    const drag = this.dragState;
    if (!drag || drag.pointerId !== event.pointerId) {
      return;
    }
    const dx = event.clientX - drag.startX;
    const dy = event.clientY - drag.startY;
    if (!drag.moved && Math.hypot(dx, dy) < DRAG_THRESHOLD_PX) {
      return;
    }
    drag.moved = true;
    this.dragging.set(true);
    this.suppressClick = true;
    this.hasCustomPos = true;
    this.fabPos.set({ left: drag.originLeft + dx, top: drag.originTop + dy });
    this.clampFabToViewport();
    this.updatePanelPlacement();
    this.syncPanelMaxHeight();
  }

  onFabPointerUp(event: PointerEvent): void {
    const drag = this.dragState;
    if (!drag || drag.pointerId !== event.pointerId) {
      return;
    }
    this.dragState = null;
    this.dragging.set(false);
    try {
      (event.currentTarget as HTMLElement).releasePointerCapture(event.pointerId);
    } catch {
      /* already released */
    }
    if (drag.moved) {
      this.clampFabToViewport();
      this.updatePanelPlacement();
      this.syncPanelMaxHeight();
      this.persistPos(this.fabPos());
    }
  }

  isDockMoved(): boolean {
    if (!this.hasCustomPos) {
      return false;
    }
    const pos = this.fabPos();
    const def = this.defaultFabPos();
    return Math.hypot(pos.left - def.left, pos.top - def.top) > 8;
  }

  resetDockPosition(event?: Event): void {
    event?.preventDefault();
    event?.stopPropagation();
    this.hasCustomPos = false;
    this.fabPos.set(this.defaultFabPos());
    this.persistPos(null);
    this.panelBelow.set(false);
    this.panelAlignStart.set(false);
    this.panelMaxHeightPx.set(null);
    this.suppressClick = true;
    afterNextRender(() => {
      this.updatePanelPlacement();
      this.syncPanelMaxHeight();
    }, { injector: this.injector });
  }

  selectTool(id: DevToolId): void {
    this.tools.open(id);
    afterNextRender(() => this.syncPanelMaxHeight(), { injector: this.injector });
  }

  panelMaxHeightStyle(): string | null {
    const px = this.panelMaxHeightPx();
    return px == null ? null : `${px}px`;
  }

  closePanel(): void {
    this.tools.close();
  }

  adjustMinusOne(): void {
    this.adjustStreak(-1);
  }

  adjustPlusOne(): void {
    this.adjustStreak(1);
  }

  adjustStreak(delta: number): void {
    this.run(() => this.gamification.adjustStreak(delta), (r) => {
      this.tools.applyStreakResult(r.currentStreak);
      return r.message;
    }, true);
  }

  applySetStreak(): void {
    this.run(
      () => this.gamification.setStreak(Number(this.tools.streakSetValue()) || 0),
      (r) => {
        this.tools.applyStreakResult(r.currentStreak);
        return r.message;
      },
      true
    );
  }

  requestResetStreak(): void {
    this.tools.pendingConfirm.set('reset-streak');
  }

  confirmResetStreak(): void {
    this.tools.pendingConfirm.set(null);
    this.run(() => this.gamification.resetStreak(), (r) => {
      this.tools.applyStreakResult(r.currentStreak);
      return r.message;
    }, true);
  }

  summon(boxType: string): void {
    this.run(
      () => this.gamification.grantLootBox(boxType),
      (box) => {
        this.lootPending.pendingBoxes.update((list) => [box, ...list]);
        return `Summoned ${box.boxType} — pending in Cache.`;
      },
      true
    );
  }

  shiftDay(delta: number): void {
    const next = this.tools.dayOffset() + delta;
    this.run(
      () => this.gamification.setDevClock({ dayOffset: next }),
      (config) => {
        this.tools.applyConfig(config);
        return `Effective today: ${config.effectiveToday} (offset ${config.dayOffset}).`;
      },
      true
    );
  }

  requestClearClock(): void {
    this.tools.pendingConfirm.set('clear-clock');
  }

  confirmClearClock(): void {
    this.tools.pendingConfirm.set(null);
    this.run(
      () => this.gamification.setDevClock({ clear: true }),
      (config) => {
        this.tools.applyConfig(config);
        return `Clock cleared. Effective today: ${config.effectiveToday}.`;
      },
      true
    );
  }

  forceClosePending(): void {
    this.run(
      () => this.gamification.forceClosePending(),
      (r) => {
        this.tools.applyStreakResult(r.streakAfter);
        return (
          `Closed pending days. Streak ${r.streakBefore} → ${r.streakAfter}` +
          (r.alreadyClosed ? ' (already closed).' : '.')
        );
      },
      true
    );
  }

  unsealToday(): void {
    this.run(
      () => this.gamification.unsealToday(),
      (r) => r.message,
      true
    );
  }

  addXp(): void {
    this.run(
      () => this.gamification.devAddXp(Number(this.tools.xpAmount()) || 0),
      (p) => {
        this.tools.applyProfile(p);
        return `XP applied. Level ${p.level} · ${p.currentXp}/${p.nextLevelXp}`;
      },
      true
    );
  }

  setLevel(): void {
    this.run(
      () => this.gamification.devSetLevel(Number(this.tools.levelValue()) || 1),
      (p) => {
        this.tools.applyProfile(p);
        return `Level set to ${p.level} (XP into level cleared).`;
      },
      true
    );
  }

  grantInventory(): void {
    const key = this.tools.selectedRewardKey();
    if (!key) {
      this.tools.setFeedback('Pick a reward first.');
      return;
    }
    this.run(
      () => this.gamification.devGrantInventory(key, Number(this.tools.grantQuantity()) || 1),
      (r) => r.message,
      true
    );
  }

  applyCombatState(): void {
    this.run(
      () =>
        this.gamification.devSetCombatState({
          streakShields: Number(this.tools.shieldsValue()) || 0,
          pendingDoubleStrike: this.tools.doubleStrike(),
          pendingSurgeToken: this.tools.surgeToken(),
        }),
      (p) => {
        this.tools.applyProfile(p);
        return (
          `Combat state saved. Shields ${this.tools.shieldsValue()}` +
          (this.tools.doubleStrike() ? ' · Double Strike armed' : '') +
          (this.tools.surgeToken() ? ' · Surge armed' : '') +
          '.'
        );
      },
      true
    );
  }

  onCampaignPicked(id: string): void {
    this.tools.selectedCampaignId.set(id);
    const current = this.tools.selectedCampaign();
    if (current) {
      this.tools.campaignPending.set(Math.max(1, Math.round(current.pendingDamage) || 10));
      this.tools.campaignProgress.set(current.currentProgress);
    }
  }

  setCampaignNearDead(): void {
    const id = this.tools.selectedCampaignId();
    const campaign = this.tools.selectedCampaign();
    if (!id || !campaign) {
      this.tools.setFeedback('Pick a campaign first.');
      return;
    }
    const progress = Math.max(0, campaign.targetValue - 1);
    this.run(
      () => this.gamification.devSetCampaignState(id, { currentProgress: progress, pendingDamage: 1 }),
      (c) => {
        this.tools.loadCampaigns();
        return `${c.name}: near-dead (${c.healthRemaining}% HP, pending ${c.pendingDamage}).`;
      },
      true
    );
  }

  setCampaignPending(): void {
    const id = this.tools.selectedCampaignId();
    if (!id) {
      this.tools.setFeedback('Pick a campaign first.');
      return;
    }
    this.run(
      () =>
        this.gamification.devSetCampaignState(id, {
          pendingDamage: Number(this.tools.campaignPending()) || 0,
        }),
      (c) => {
        this.tools.loadCampaigns();
        return `${c.name}: pending Strike bank ${c.pendingDamage}.`;
      },
      true
    );
  }

  forceStrike(): void {
    const id = this.tools.selectedCampaignId();
    if (!id) {
      this.tools.setFeedback('Pick a campaign first.');
      return;
    }
    this.run(
      () => this.gamification.devStrikeCampaign(id),
      (r) => {
        this.tools.loadCampaigns();
        return r.goalCompleted
          ? `Slain ${r.goalName} (+${r.goalXpReward} XP).`
          : `Struck ${r.goalName} for ${r.progressDelta} · ${r.healthRemaining}% HP left.`;
      },
      true
    );
  }

  subscribeAll(): void {
    const id = this.tools.selectedCampaignId();
    if (!id) {
      this.tools.setFeedback('Pick a campaign first.');
      return;
    }
    this.run(
      () => this.gamification.devSubscribeAllCategories(id),
      (r) => {
        this.tools.loadCampaigns();
        return r.message;
      },
      true
    );
  }

  cancelConfirm(): void {
    this.tools.pendingConfirm.set(null);
  }

  private run<T>(
    action: () => import('rxjs').Observable<T>,
    message: (value: T) => string,
    notify = false
  ): void {
    if (this.tools.busy()) {
      return;
    }
    this.tools.busy.set(true);
    action().subscribe({
      next: (value) => {
        this.tools.setFeedback(message(value));
        this.tools.busy.set(false);
        if (notify) {
          this.tools.notifyDataChanged();
        }
      },
      error: (err) => {
        const msg = (err as { error?: { message?: string }; status?: number })?.error?.message;
        const status = (err as { status?: number })?.status;
        this.tools.setFeedback(
          msg ?? (status === 403 ? 'Dev tools disabled on this profile.' : 'Action failed.')
        );
        this.tools.busy.set(false);
      },
    });
  }

  private readStoredPos(): { left: number; top: number } | null {
    try {
      const raw = localStorage.getItem(DOCK_POS_KEY);
      if (!raw) {
        return null;
      }
      const parsed = JSON.parse(raw) as { left?: unknown; top?: unknown };
      const left = typeof parsed.left === 'number' && Number.isFinite(parsed.left) ? parsed.left : null;
      const top = typeof parsed.top === 'number' && Number.isFinite(parsed.top) ? parsed.top : null;
      if (left == null || top == null) {
        return null;
      }
      return { left, top };
    } catch {
      return null;
    }
  }

  private persistPos(pos: { left: number; top: number } | null): void {
    try {
      if (!pos) {
        localStorage.removeItem(DOCK_POS_KEY);
        return;
      }
      localStorage.setItem(DOCK_POS_KEY, JSON.stringify(pos));
    } catch {
      /* ignore quota / private mode */
    }
  }

  private roughDefaultFabPos(): { left: number; top: number } {
    if (typeof window === 'undefined') {
      return { left: 0, top: 0 };
    }
    return {
      left: Math.max(DOCK_MARGIN_PX, window.innerWidth - 116),
      top: Math.max(DOCK_MARGIN_PX, window.innerHeight - 68),
    };
  }

  private defaultFabPos(): { left: number; top: number } {
    const { w, h } = this.fabSize();
    return {
      left: Math.max(DOCK_MARGIN_PX, window.innerWidth - w - DOCK_MARGIN_PX),
      top: Math.max(DOCK_MARGIN_PX, window.innerHeight - h - DOCK_MARGIN_PX),
    };
  }

  private fabSize(): { w: number; h: number } {
    const fab = this.hostEl.nativeElement.querySelector('.dev-fab') as HTMLElement | null;
    return { w: fab?.offsetWidth || 96, h: fab?.offsetHeight || 48 };
  }

  /**
   * Prefer panel above + right-aligned to the FAB.
   * Flip below / start-aligned when that would clip outside the viewport.
   */
  private updatePanelPlacement(): void {
    if (typeof window === 'undefined') {
      return;
    }
    const pos = this.fabPos();
    const { w, h } = this.fabSize();
    const gap = 12;
    const pad = 8;
    const spaceAbove = Math.max(0, pos.top - gap);
    const spaceBelow = Math.max(0, window.innerHeight - (pos.top + h) - gap);
    const preferred = Math.min(window.innerHeight * 0.55, 28 * 16);
    const need = Math.min(preferred, 300);
    this.panelBelow.set(spaceAbove < need && spaceBelow > spaceAbove);

    const panelW = Math.min(26 * 16, window.innerWidth - 2.5 * 16);
    const rightAlignedLeft = pos.left + w - panelW;
    this.panelAlignStart.set(rightAlignedLeft < pad);
  }

  private syncPanelMaxHeight(): void {
    if (typeof window === 'undefined' || !this.tools.openTool()) {
      this.panelMaxHeightPx.set(null);
      return;
    }
    const pos = this.fabPos();
    const { h } = this.fabSize();
    const gap = 12;
    const hardCap = Math.min(window.innerHeight * 0.85, 40 * 16);
    const available = this.panelBelow()
      ? window.innerHeight - (pos.top + h) - gap
      : pos.top - gap;
    this.panelMaxHeightPx.set(Math.max(160, Math.min(hardCap, available)));
  }

  /** Keep the FAB itself on-screen; panel scrolls within max-height. */
  private clampFabToViewport(): void {
    if (typeof window === 'undefined') {
      return;
    }
    const { w, h } = this.fabSize();
    const pad = 8;
    const pos = this.fabPos();
    const left = Math.min(Math.max(pos.left, pad), Math.max(pad, window.innerWidth - w - pad));
    const top = Math.min(Math.max(pos.top, pad), Math.max(pad, window.innerHeight - h - pad));
    if (left !== pos.left || top !== pos.top) {
      this.fabPos.set({ left, top });
    }
  }
}
