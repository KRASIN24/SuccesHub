import { Component } from '@angular/core';
import { DecimalPipe } from '@angular/common';

interface ProtocolTask {
  title: string;
  detail: string;
  xp: number;
  done: boolean;
}

interface Merit {
  label: string;
  icon: string;
  locked: boolean;
}

interface IndexRow {
  pair: string;
  price: string;
  change: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent {
  readonly level = 42;
  readonly currentXp = 2450;
  readonly nextLevelXp = 3000;

  get xpPercent(): number {
    return Math.round((this.currentXp / this.nextLevelXp) * 100);
  }

  readonly tasks: ProtocolTask[] = [
    {
      title: 'Deep Work: Interface Refinement',
      detail: 'Focus on the editorial typography scale.',
      xp: 450,
      done: false,
    },
    {
      title: 'Review Design System Guidelines',
      detail: 'Apply the "No-Line" rule across all panels.',
      xp: 200,
      done: false,
    },
    {
      title: 'Hydration & Movement Protocol',
      detail: 'System maintenance required.',
      xp: 50,
      done: true,
    },
  ];

  readonly merits: Merit[] = [
    { label: 'Speedster', icon: 'bolt', locked: false },
    { label: 'Pioneer', icon: 'explore', locked: false },
    { label: 'Archivist', icon: 'diamond', locked: true },
  ];

  readonly indexRows: IndexRow[] = [
    { pair: 'BTC/USD', price: '64,291.50', change: '+2.4%' },
    { pair: 'ETH/USD', price: '3,412.12', change: '+0.8%' },
  ];

  get tasksRemaining(): number {
    return this.tasks.filter((t) => !t.done).length + 3;
  }

  toggleTask(task: ProtocolTask): void {
    task.done = !task.done;
  }
}
