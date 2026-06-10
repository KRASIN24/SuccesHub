import { Component } from '@angular/core';

interface Boss {
  tier: string;
  name: string;
  xp: number;
  health: number;
  target: string;
  icon: string;
  action: string;
  featured: boolean;
}

interface ConqueredFoe {
  name: string;
  slain: string;
  icon: string;
}

@Component({
  selector: 'app-goals',
  standalone: true,
  imports: [],
  templateUrl: './goals.component.html',
  styleUrl: './goals.component.scss',
})
export class GoalsComponent {
  readonly bosses: Boss[] = [
    {
      tier: 'Level 42 Beast',
      name: 'Financial Titan',
      xp: 5000,
      health: 88,
      target: '$100k Savings',
      icon: 'savings',
      action: 'Attack',
      featured: false,
    },
    {
      tier: 'Ancient Guardian',
      name: 'The Marathoner',
      xp: 2500,
      health: 12,
      target: '42.2KM Run',
      icon: 'directions_run',
      action: 'Final Strike',
      featured: true,
    },
    {
      tier: 'Void Wyrm',
      name: 'Project Genesis',
      xp: 9000,
      health: 95,
      target: 'App Launch',
      icon: 'rocket_launch',
      action: 'Attack',
      featured: false,
    },
  ];

  readonly conquered: ConqueredFoe[] = [
    { name: 'Debt Phantom', slain: 'Slain Dec 2023', icon: 'account_balance' },
    { name: 'Procrastination Hydra', slain: 'Slain Jan 2024', icon: 'schedule' },
    { name: 'Public Speaking Golem', slain: 'Slain Feb 2024', icon: 'record_voice_over' },
    { name: 'Smoking Chimera', slain: 'Slain Mar 2024', icon: 'smoke_free' },
  ];

  readonly grandCampaign = 75;

  progress(boss: Boss): number {
    return 100 - boss.health;
  }
}
