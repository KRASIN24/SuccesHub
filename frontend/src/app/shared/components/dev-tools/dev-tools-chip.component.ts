import { CommonModule } from '@angular/common';
import { Component, inject, input } from '@angular/core';
import { DevToolId, DevToolsService } from '../../../core/services/dev-tools.service';

const LABELS: Record<DevToolId, { label: string; icon: string }> = {
  streak: { label: 'Streak', icon: 'local_fire_department' },
  cache: { label: 'Cache', icon: 'inventory_2' },
  time: { label: 'Time', icon: 'schedule' },
  ritual: { label: 'Ritual', icon: 'auto_awesome' },
};

/**
 * Page-local messenger chip that opens the matching tool in the global Dev panel.
 */
@Component({
  selector: 'app-dev-tools-chip',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (tools.enabled()) {
      <button
        type="button"
        class="dev-chip"
        [class.dev-chip--active]="tools.openTool() === tool()"
        (click)="tools.toggle(tool())"
        [attr.aria-label]="'Open ' + meta.label + ' tools'"
      >
        <span class="material-symbols-outlined">{{ meta.icon }}</span>
        <span>{{ meta.label }}</span>
      </button>
    }
  `,
  styles: [
    `
      :host {
        display: inline-flex;
      }

      .dev-chip {
        display: inline-flex;
        align-items: center;
        gap: 0.35rem;
        border: 1px solid rgba(255, 255, 255, 0.12);
        border-radius: 999px;
        padding: 0.35rem 0.7rem;
        background: linear-gradient(160deg, rgba(29, 36, 48, 0.95), rgba(18, 22, 29, 0.95));
        color: #d7dde8;
        font: inherit;
        font-size: 0.75rem;
        font-weight: 600;
        cursor: pointer;
        box-shadow: 0 6px 16px rgba(0, 0, 0, 0.25);
      }

      .dev-chip .material-symbols-outlined {
        font-size: 1rem;
      }

      .dev-chip--active {
        border-color: rgba(255, 171, 145, 0.4);
        color: #fff4e8;
      }
    `,
  ],
})
export class DevToolsChipComponent {
  readonly tools = inject(DevToolsService);
  readonly tool = input.required<DevToolId>();

  get meta(): { label: string; icon: string } {
    return LABELS[this.tool()];
  }
}
