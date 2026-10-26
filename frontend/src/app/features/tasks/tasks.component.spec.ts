import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { NEVER, of } from 'rxjs';
import { GamificationCelebrationService } from '../../core/services/gamification-celebration.service';
import { GamificationService } from '../../core/services/gamification.service';
import { GoalService } from '../../core/services/goal.service';
import { TaskService } from '../../core/services/task.service';
import { TranslateService } from '../../core/i18n/translate.service';
import { Task } from '../../core/models/task.model';
import { TasksComponent } from './tasks.component';

describe('TasksComponent', () => {
  let taskService: jasmine.SpyObj<TaskService>;

  beforeEach(async () => {
    taskService = jasmine.createSpyObj<TaskService>('TaskService', [
      'deleteTask',
      'deleteCategory',
    ]);
    taskService.deleteTask.and.returnValue(of(undefined));
    taskService.deleteCategory.and.returnValue(NEVER);

    await TestBed.configureTestingModule({
      imports: [TasksComponent],
      providers: [
        { provide: TaskService, useValue: taskService },
        { provide: GoalService, useValue: {} },
        { provide: GamificationService, useValue: {} },
        { provide: GamificationCelebrationService, useValue: {} },
        { provide: TranslateService, useValue: { t: (key: string) => key } },
        { provide: ActivatedRoute, useValue: {} },
      ],
    }).compileComponents();
  });

  it('deletes only category roots because parent deletion cascades to subtasks', () => {
    const component = TestBed.createComponent(TasksComponent).componentInstance;
    component.allTasks.set([
      task('root-1', 'category-1'),
      task('child-1', 'category-1', 'root-1'),
      task('other-root', 'category-2'),
    ]);

    component.deleteCategory('category-1');

    expect(taskService.deleteTask).toHaveBeenCalledTimes(1);
    expect(taskService.deleteTask).toHaveBeenCalledWith('root-1');
    expect(taskService.deleteCategory).toHaveBeenCalledWith('category-1');
  });

  function task(id: string, categoryId: string, parentTaskId: string | null = null): Task {
    return {
      id,
      categoryId,
      parentTaskId,
      starred: false,
      sortOrder: 0,
      title: id,
      xpReward: 0,
      difficulty: 1,
      durationMinutes: 5,
      priority: 1,
      weeklyChallenge: false,
      status: 'TODO',
      createdAt: '2026-10-26T00:00:00Z',
    };
  }
});
