import { Component, model, output } from '@angular/core';

/**
 * Animated sun/moon theme toggle (dark = checked/night).
 * Reusable for Settings and future chrome.
 */
@Component({
  selector: 'app-theme-toggle',
  standalone: true,
  templateUrl: './theme-toggle.component.html',
  styleUrl: './theme-toggle.component.scss',
})
export class ThemeToggleComponent {
  /** true = dark / night mode */
  readonly dark = model(true);
  readonly darkChange = output<boolean>();

  onChange(event: Event): void {
    const next = (event.target as HTMLInputElement).checked;
    this.dark.set(next);
    this.darkChange.emit(next);
  }
}
