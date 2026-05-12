import { Component, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';
import { MatRippleModule } from '@angular/material/core';
import { AuthService } from '../../core/services/auth.service';

interface StatCard {
  label: string;
  value: string;
  icon: string;
  color: string;
  trend: string;
  trendUp: boolean;
}

interface ActivityItem {
  icon: string;
  title: string;
  description: string;
  time: string;
  color: string;
}

interface QuickAction {
  label: string;
  icon: string;
  color: string;
  route: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatDividerModule,
    MatRippleModule,
  ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent {
  protected readonly authService = inject(AuthService);

  readonly stats: StatCard[] = [
    {
      label: 'Total Goals',
      value: '12',
      icon: 'flag',
      color: '#3b82f6',
      trend: '+2 this month',
      trendUp: true,
    },
    {
      label: 'Tasks Completed',
      value: '48',
      icon: 'task_alt',
      color: '#10b981',
      trend: '+8 this week',
      trendUp: true,
    },
    {
      label: 'Achievements',
      value: '7',
      icon: 'emoji_events',
      color: '#f59e0b',
      trend: '+1 new',
      trendUp: true,
    },
    {
      label: 'Active Streak',
      value: '14d',
      icon: 'local_fire_department',
      color: '#ef4444',
      trend: 'Personal best!',
      trendUp: true,
    },
  ];

  readonly quickActions: QuickAction[] = [
    { label: 'Create Goal', icon: 'add_circle', color: '#3b82f6', route: '/goals/new' },
    { label: 'Add Task', icon: 'playlist_add', color: '#10b981', route: '/tasks/new' },
    { label: 'View Reports', icon: 'bar_chart', color: '#8b5cf6', route: '/reports' },
  ];

  readonly recentActivity: ActivityItem[] = [
    {
      icon: 'task_alt',
      title: 'Task completed',
      description: 'Finished reading "Atomic Habits" chapter 5',
      time: '2 hours ago',
      color: '#10b981',
    },
    {
      icon: 'flag',
      title: 'Goal updated',
      description: 'Run a 5K — progress updated to 70%',
      time: '5 hours ago',
      color: '#3b82f6',
    },
    {
      icon: 'emoji_events',
      title: 'Achievement unlocked',
      description: '7-day streak — "Week Warrior" badge earned',
      time: 'Yesterday',
      color: '#f59e0b',
    },
    {
      icon: 'add_circle',
      title: 'New goal created',
      description: 'Learn Angular 18 — deadline set for June 30',
      time: '2 days ago',
      color: '#8b5cf6',
    },
  ];

  readonly today = new Date().toLocaleDateString('en-US', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  get greeting(): string {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 17) return 'Good afternoon';
    return 'Good evening';
  }
}
