import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { StarStudent } from './dashboard.model';

@Component({
  selector: 'app-star-students',
  imports: [TranslateModule],
  template: `
    @if (students.length === 0) {
      <p class="empty">{{ 'dashboard.starStudents.empty' | translate }}</p>
    } @else {
      <ul class="list">
        @for (student of students; track student.studentId; let i = $index) {
          <li class="row">
            <span class="rank" [class.top]="i === 0">{{ i + 1 }}</span>
            <span class="avatar" [class]="'badge-tone-' + student.badge.toLowerCase()">
              {{ initials(student.name) }}
            </span>
            <div class="info">
              <span class="name">{{ student.name }}</span>
              <span class="meta">
                {{ student.className || '—' }}@if (student.sectionName) {<span> · {{ student.sectionName }}</span>}
              </span>
              <span class="achievement">{{ student.achievement }}</span>
            </div>
            <div class="side">
              <span class="badge" [class]="'badge-tone-' + student.badge.toLowerCase()">
                {{ 'dashboard.starStudents.badges.' + student.badge | translate }}
              </span>
              <span class="points">{{ student.points }} {{ 'dashboard.starStudents.points' | translate }}</span>
            </div>
          </li>
        }
      </ul>
    }
  `,
  styles: `
    :host { display: block; }
    .empty { color: var(--color-muted); padding: 24px 0; text-align: center; }
    .list { list-style: none; margin: 0; padding: 0; }
    .row {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 0;
      border-bottom: 1px solid var(--color-border);
    }
    .row:last-child { border-bottom: none; }
    .rank {
      width: 22px; height: 22px;
      display: inline-flex; align-items: center; justify-content: center;
      border-radius: 50%;
      font-size: .72rem; font-weight: 700;
      background: var(--color-bg);
      color: var(--color-muted);
      flex-shrink: 0;
    }
    .rank.top { background: #fef3c7; color: #b45309; }
    .avatar {
      width: 40px; height: 40px;
      display: inline-flex; align-items: center; justify-content: center;
      border-radius: 50%;
      font-weight: 700; font-size: .85rem;
      color: #fff;
      flex-shrink: 0;
      background: var(--color-primary);
    }
    .avatar.badge-tone-gold { background: linear-gradient(135deg, #f59e0b, #d97706); }
    .avatar.badge-tone-silver { background: linear-gradient(135deg, #94a3b8, #64748b); }
    .avatar.badge-tone-bronze { background: linear-gradient(135deg, #c2703f, #92400e); }
    .avatar.badge-tone-star { background: linear-gradient(135deg, #8b5cf6, #6d28d9); }
    .info { flex: 1; display: flex; flex-direction: column; min-width: 0; }
    .name { font-weight: 600; }
    .meta { font-size: .78rem; color: var(--color-muted); }
    .achievement {
      font-size: .8rem;
      color: var(--color-text);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .side { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; flex-shrink: 0; }
    .badge-tone-gold { background: #fef3c7; color: #b45309; }
    .badge-tone-silver { background: #e2e8f0; color: #475569; }
    .badge-tone-bronze { background: #fde8d7; color: #9a3412; }
    .badge-tone-star { background: #ede9fe; color: #6d28d9; }
    .points { font-size: .72rem; color: var(--color-muted); }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StarStudentsComponent {
  @Input() students: StarStudent[] = [];

  initials(name: string): string {
    const parts = name.trim().split(/\s+/).filter(Boolean);
    if (parts.length === 0) {
      return '?';
    }
    return parts
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('');
  }
}
