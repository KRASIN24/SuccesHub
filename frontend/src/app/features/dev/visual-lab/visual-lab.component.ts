import { CommonModule } from '@angular/common';
import { Component, ViewEncapsulation, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ThemeService } from '../../../core/services/theme.service';
import { GlitchButtonComponent } from '../../../shared/components/glitch-button/glitch-button.component';
import { GlitchCheckboxComponent } from '../../../shared/components/glitch-checkbox/glitch-checkbox.component';
import { VisualLabPicksService } from './visual-lab-picks.service';
import { VISUAL_LAB_GROUPS } from './visual-lab-specimens';

/**
 * Temporary gated gallery of competing UI styles with provenance.
 * Operators pick winners; W15 applies them product-wide.
 */
@Component({
  selector: 'app-visual-lab',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    GlitchButtonComponent,
    GlitchCheckboxComponent,
  ],
  templateUrl: './visual-lab.component.html',
  styleUrl: './visual-lab.component.scss',
  encapsulation: ViewEncapsulation.None,
})
export class VisualLabComponent {
  readonly groups = VISUAL_LAB_GROUPS;
  readonly picks = inject(VisualLabPicksService);
  readonly theme = inject(ThemeService);

  readonly copyStatus = signal<string | null>(null);
  readonly glitchCheck = signal(true);

  select(groupId: string, specimenId: string): void {
    this.picks.setPick(groupId, specimenId);
  }

  isSelected(groupId: string, specimenId: string): boolean {
    return this.picks.picks()[groupId] === specimenId;
  }

  toggleTheme(): void {
    this.theme.setDark(!this.theme.isDark());
  }

  async copyPicks(): Promise<void> {
    try {
      await navigator.clipboard.writeText(this.picks.exportJson());
      this.copyStatus.set('Copied picks JSON');
    } catch {
      this.copyStatus.set('Copy failed — use Export below');
    }
    setTimeout(() => this.copyStatus.set(null), 2500);
  }

  clearPicks(): void {
    this.picks.clear();
    this.copyStatus.set('Reset to recommended picks');
    setTimeout(() => this.copyStatus.set(null), 2500);
  }

  scrollTo(groupId: string): void {
    document.getElementById(`vl-group-${groupId}`)?.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
    });
  }
}
