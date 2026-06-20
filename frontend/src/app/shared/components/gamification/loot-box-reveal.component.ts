import { Component, Input, signal } from '@angular/core';
import { RewardItem } from '../../../core/models/gamification.model';

@Component({
  selector: 'app-loot-box-reveal',
  standalone: true,
  template: `
    @if (visible()) {
      <div class="loot-overlay" (click)="dismiss()">
        <div class="loot-cards" (click)="$event.stopPropagation()">
          <h3>Loot Drop!</h3>
          @for (item of items(); track item.id) {
            <div class="loot-card" [class.legendary]="item.rarity === 'LEGENDARY'">
              <span class="material-icons">{{ item.icon }}</span>
              <span>{{ item.label }}</span>
            </div>
          }
          <button type="button" class="loot-close" (click)="dismiss()">Collect</button>
        </div>
      </div>
    }
  `,
  styles: [`
    .loot-overlay {
      position: fixed;
      inset: 0;
      background: rgba(0,0,0,0.8);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1200;
    }
    .loot-cards { text-align: center; padding: 32px; }
    .loot-card {
      display: inline-flex;
      flex-direction: column;
      align-items: center;
      margin: 8px;
      padding: 16px;
      border: 1px solid rgba(255,255,255,0.2);
      border-radius: 8px;
      animation: flip 0.5s ease;
    }
    .loot-card.legendary {
      border-color: #ffd700;
      box-shadow: 0 0 20px rgba(255,215,0,0.5);
    }
    .loot-close {
      margin-top: 16px;
      padding: 8px 24px;
      background: var(--color-gold, #d4af37);
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-weight: 600;
    }
    @keyframes flip {
      0% { transform: rotateY(90deg); opacity: 0; }
      100% { transform: rotateY(0); opacity: 1; }
    }
  `],
})
export class LootBoxRevealComponent {
  readonly visible = signal(false);
  readonly items = signal<RewardItem[]>([]);

  @Input() set trigger(value: { items: RewardItem[]; tick: number } | null) {
    if (!value || value.items.length === 0) return;
    this.items.set(value.items);
    this.visible.set(true);
  }

  dismiss(): void {
    this.visible.set(false);
  }
}
