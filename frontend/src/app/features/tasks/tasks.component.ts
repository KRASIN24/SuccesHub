import { Component } from '@angular/core';

type BadgeType = 'urgent' | 'due' | 'ongoing' | 'time';

interface QuestTask {
  title: string;
  desc?: string;
  meta?: string;
  metaType?: BadgeType;
  xp?: number;
  done?: boolean;
}

interface QuestColumn {
  name: string;
  tag: string;
  grantXp: boolean;
  muted?: boolean;
  tasks: QuestTask[];
}

@Component({
  selector: 'app-tasks',
  standalone: true,
  imports: [],
  templateUrl: './tasks.component.html',
  styleUrl: './tasks.component.scss',
})
export class TasksComponent {
  readonly columns: QuestColumn[] = [
    {
      name: 'Daily Protocol',
      tag: 'Health',
      grantXp: true,
      tasks: [
        { title: 'Hydration Sequence Alpha', meta: '8:00 AM', metaType: 'time', xp: 50 },
        { title: 'Focus Meditation: 15min', meta: '8:30 AM', metaType: 'time', xp: 100 },
      ],
    },
    {
      name: 'Strategic Missions',
      tag: 'Career',
      grantXp: true,
      tasks: [
        {
          title: 'Q3 Market Analysis Review',
          desc: 'Complete deep dive into competitor pricing structures and global logistics…',
          meta: 'Urgent',
          metaType: 'urgent',
          xp: 850,
        },
        {
          title: 'Secure Venture Funding Deck',
          meta: 'Due Mon',
          metaType: 'due',
          xp: 1200,
        },
      ],
    },
    {
      name: 'Creative Mastery',
      tag: 'Creative Mastery',
      grantXp: false,
      muted: true,
      tasks: [{ title: 'Visual Identity Refinement', meta: 'Ongoing', metaType: 'ongoing' }],
    },
    {
      name: 'Finance Ops',
      tag: 'Finance',
      grantXp: true,
      tasks: [{ title: 'Audit Portfolio Diversification', done: true }],
    },
  ];

  toggleGrantXp(column: QuestColumn): void {
    column.grantXp = !column.grantXp;
  }

  toggleTask(task: QuestTask): void {
    task.done = !task.done;
  }
}
