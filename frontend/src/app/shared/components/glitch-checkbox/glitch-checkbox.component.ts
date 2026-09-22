import { Component, input, model, output } from '@angular/core';

/**
 * Reusable glitch-styled checkbox for future UI (Settings prefs, toggles, etc.).
 * Uses SuccessHub gold accents instead of the cyan/magenta sample palette.
 */
@Component({
  selector: 'app-glitch-checkbox',
  standalone: true,
  templateUrl: './glitch-checkbox.component.html',
  styleUrl: './glitch-checkbox.component.scss',
})
export class GlitchCheckboxComponent {
  /** Visible label text (uppercase recommended). */
  readonly label = input.required<string>();
  /** Optional override for glitch hover text; defaults to label. */
  readonly glitchText = input<string | null>(null);
  readonly disabled = input(false);
  readonly checked = model(false);
  readonly checkedChange = output<boolean>();

  onChange(event: Event): void {
    if (this.disabled()) return;
    const next = (event.target as HTMLInputElement).checked;
    this.checked.set(next);
    this.checkedChange.emit(next);
  }

  displayGlitchText(): string {
    return this.glitchText() ?? this.label();
  }
}
