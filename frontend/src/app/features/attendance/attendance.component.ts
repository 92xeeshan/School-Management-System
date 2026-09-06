import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { ApiResponse } from '../../core/models/api.model';
import { AuthService } from '../../core/auth/auth.service';

interface AcademicYear {
  id: string;
  name: string;
  current: boolean;
}

interface Section {
  id: string;
  classId: string;
  className: string;
  name: string;
  capacity: number;
}

interface StudentRow {
  id: string;
  admissionNo: string;
  firstName: string;
  lastName: string;
  className: string;
  sectionName: string;
  rollNumber: number;
}

interface BackendStudentListItem {
  student: { id: string; admissionNo: string; firstName: string; lastName: string };
  className: string;
  sectionName: string;
  rollNumber: number;
}

interface AttendanceRow {
  studentId: string;
  studentName: string;
  admissionNo: string;
  status: Status;
}

type Status = 'PRESENT' | 'ABSENT' | 'LATE' | 'LEAVE';

const STATUSES: Status[] = ['PRESENT', 'ABSENT', 'LATE', 'LEAVE'];

@Component({
  selector: 'app-attendance',
  imports: [TranslateModule, ReactiveFormsModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'attendance.title' | translate }}</h1>
          <p class="muted">{{ 'attendance.subtitle' | translate }}</p>
        </div>
      </div>

      <div class="card">
        <div class="toolbar">
          <div class="field">
            <label>{{ 'attendance.selectClass' | translate }}</label>
            <select [formControl]="classControl" (change)="onClassChange()">
              @for (klass of classes; track klass.id) {
                <option [value]="klass.id">{{ klass.name }}</option>
              }
            </select>
          </div>
          <div class="field">
            <label>{{ 'attendance.selectSection' | translate }}</label>
            <select [formControl]="sectionControl" (change)="onSectionChange()">
              @for (section of sectionsForSelectedClass; track section.id) {
                <option [value]="section.id">{{ section.name }}</option>
              }
            </select>
          </div>
          <div class="field">
            <label>{{ 'attendance.date' | translate }}</label>
            <input type="date" [formControl]="dateControl" (change)="loadForSelectedSection()" />
          </div>
          @if (canMark) {
            <div class="field field-btn">
              <label>&nbsp;</label>
              <button class="btn btn-primary" (click)="onSave()" [disabled]="saving">
                {{ saving ? ('common.loading' | translate) : ('attendance.saveAttendance' | translate) }}
              </button>
            </div>
          }
        </div>
      </div>

      <div class="card">
        <div class="summary">
          <span class="chip chip-present">{{ 'attendance.present' | translate }}: {{ counts.PRESENT }}</span>
          <span class="chip chip-absent">{{ 'attendance.absent' | translate }}: {{ counts.ABSENT }}</span>
          <span class="chip chip-late">{{ 'attendance.late' | translate }}: {{ counts.LATE }}</span>
          <span class="chip chip-leave">{{ 'attendance.leave' | translate }}: {{ counts.LEAVE }}</span>
          <span class="muted year">{{ academicYearLabel }}</span>
        </div>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ 'students.rollNumber' | translate }}</th>
                <th>{{ 'students.studentId' | translate }}</th>
                <th>{{ 'students.name' | translate }}</th>
                <th>{{ 'common.status' | translate }}</th>
              </tr>
            </thead>
            <tbody>
              @for (row of rows; track row.studentId; let i = $index) {
                <tr>
                  <td>{{ rollFor(row.studentId) }}</td>
                  <td>{{ admissionFor(row.studentId) }}</td>
                  <td class="strong">{{ row.studentName }}</td>
                  <td>
                    @if (canMark) {
                      <div class="status-group">
                        @for (status of statuses; track status) {
                          <button
                            class="status-btn"
                            [class.selected]="row.status === status"
                            [attr.data-status]="status"
                            (click)="setStatus(row, status)">
                            {{ 'attendance.' + status.toLowerCase() | translate }}
                          </button>
                        }
                      </div>
                    } @else {
                      {{ 'attendance.' + row.status.toLowerCase() | translate }}
                    }
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="4" class="center">{{ 'common.noData' | translate }}</td></tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
  styles: `
    .page-header { margin-bottom: 20px; }
    h1 { font-size: 1.5rem; margin: 0 0 4px; }
    .muted { color: var(--color-muted); }
    .toolbar { display: flex; gap: 16px; flex-wrap: wrap; padding: 16px; }
    .field { display: flex; flex-direction: column; gap: 6px; }
    .field label { font-weight: 500; font-size: .85rem; color: var(--color-muted); }
    select, input {
      padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; min-width: 150px;
    }
    .field-btn { justify-content: flex-end; }
    .summary { display: flex; gap: 12px; flex-wrap: wrap; align-items: center; padding: 16px; border-bottom: 1px solid var(--color-border); }
    .year { font-size: .85rem; }
    .chip { padding: 6px 14px; border-radius: 20px; font-size: .85rem; font-weight: 600; }
    .chip-present { background: #dcfce7; color: #15803d; }
    .chip-absent { background: #fee2e2; color: #b91c1c; }
    .chip-late { background: #fef3c7; color: #b45309; }
    .chip-leave { background: #e0e7ff; color: #4338ca; }
    .table-wrap { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: 12px 16px; border-bottom: 1px solid var(--color-border); font-size: .92rem; }
    th { color: var(--color-muted); font-weight: 600; font-size: .8rem; text-transform: uppercase; letter-spacing: .03em; background: var(--color-bg); }
    .strong { font-weight: 600; }
    .center { text-align: center; color: var(--color-muted); padding: 28px; }
    .status-group { display: flex; gap: 6px; flex-wrap: wrap; }
    .status-btn {
      padding: 5px 12px; border-radius: 8px; border: 1px solid var(--color-border);
      background: #fff; cursor: pointer; font-size: .82rem; transition: all .12s ease;
    }
    .status-btn:hover { border-color: var(--color-primary); }
    .status-btn.selected[data-status="PRESENT"] { background: #15803d; color: #fff; border-color: #15803d; }
    .status-btn.selected[data-status="ABSENT"] { background: #b91c1c; color: #fff; border-color: #b91c1c; }
    .status-btn.selected[data-status="LATE"] { background: #b45309; color: #fff; border-color: #b45309; }
    .status-btn.selected[data-status="LEAVE"] { background: #4338ca; color: #fff; border-color: #4338ca; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AttendanceComponent implements OnInit {
  readonly statuses = STATUSES;
  readonly classControl = new FormControl('');
  readonly sectionControl = new FormControl('');
  readonly dateControl = new FormControl(new Date().toISOString().slice(0, 10));

  classes: Array<{ id: string; name: string }> = [];
  sections: Section[] = [];
  academicYears: AcademicYear[] = [];
  students: StudentRow[] = [];
  rows: AttendanceRow[] = [];
  saving = false;

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef, private auth: AuthService) {}

  get canMark(): boolean {
    return this.auth.hasPermission('ATTENDANCE_MARK');
  }

  get sectionsForSelectedClass(): Section[] {
    const classId = this.classControl.value;
    return this.sections.filter((s) => s.classId === classId);
  }

  get currentAcademicYear(): AcademicYear | null {
    return this.academicYears.find((y) => y.current) ?? this.academicYears[0] ?? null;
  }

  get academicYearLabel(): string {
    return this.currentAcademicYear?.name ?? '';
  }

  get counts(): Record<Status, number> {
    const base: Record<Status, number> = { PRESENT: 0, ABSENT: 0, LATE: 0, LEAVE: 0 };
    for (const row of this.rows) {
      base[row.status] += 1;
    }
    return base;
  }

  ngOnInit(): void {
    this.loadMeta();
  }

  onClassChange(): void {
    const section = this.sectionsForSelectedClass[0];
    this.sectionControl.setValue(section?.id ?? '', { emitEvent: false });
    this.loadForSelectedSection();
  }

  onSectionChange(): void {
    this.loadForSelectedSection();
  }

  setStatus(row: AttendanceRow, status: Status): void {
    row.status = status;
  }

  rollFor(studentId: string): number {
    return this.students.find((s) => s.id === studentId)?.rollNumber ?? 0;
  }

  admissionFor(studentId: string): string {
    return this.students.find((s) => s.id === studentId)?.admissionNo ?? '';
  }

  private loadMeta(): void {
    this.http.get<ApiResponse<AcademicYear[]>>('/api/academic-years').subscribe({
      next: (res) => {
        this.academicYears = res.data;
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
    this.http.get<ApiResponse<Section[]>>('/api/sections').subscribe({
      next: (res) => {
        this.sections = res.data;
        const classIds = Array.from(new Set(res.data.map((s) => s.classId)));
        this.http.get<ApiResponse<Array<{ id: string; name: string }>>>('/api/classes').subscribe({
          next: (classesRes) => {
            this.classes = classesRes.data;
            this.cdr.markForCheck();
            const first = this.classes[0];
            if (first) {
              this.classControl.setValue(first.id, { emitEvent: false });
              this.onClassChange();
            }
          },
          error: () => {
            this.classes = classIds.map((id) => ({ id, name: id }));
            const first = this.classes[0];
            if (first) {
              this.classControl.setValue(first.id, { emitEvent: false });
              this.onClassChange();
            }
          },
        });
      },
      error: () => undefined,
    });
  }

  loadForSelectedSection(): void {    const sectionId = this.sectionControl.value;
    const academicYearId = this.currentAcademicYear?.id;
    const date = this.dateControl.value;
    if (!sectionId || !academicYearId) {
      return;
    }
    this.loadStudents(sectionId, academicYearId);
    this.http
      .get<ApiResponse<Array<{ studentId: string; studentName: string; status: string }>>>(
        '/api/attendance/section',
        { params: { sectionId, academicYearId, date: date ?? '' } }
      )
      .subscribe({
        next: (res) => {
          this.applyExistingRecords(res.data);
          this.cdr.markForCheck();
        },
        error: () => undefined,
      });
  }

  private applyExistingRecords(records: Array<{ studentId: string; studentName: string; status: string }>): void {
    if (records.length === 0) {
      return;
    }
    const map = new Map(records.filter((r) => r.status).map((r) => [r.studentId, r.status as Status]));
    for (const row of this.rows) {
      if (map.has(row.studentId)) {
        row.status = map.get(row.studentId) as Status;
      }
    }
  }

  private loadStudents(sectionId: string, academicYearId: string): void {
    this.http
      .get<ApiResponse<BackendStudentListItem[]>>('/api/students/by-section', {
        params: { sectionId, academicYearId },
      })
      .subscribe({
        next: (res) => {
          this.students = res.data.map((item) => ({
            id: item.student.id,
            admissionNo: item.student.admissionNo,
            firstName: item.student.firstName,
            lastName: item.student.lastName,
            className: item.className,
            sectionName: item.sectionName,
            rollNumber: item.rollNumber ?? 0,
          }));
          this.rows = this.students.map((s) => ({
            studentId: s.id,
            studentName: `${s.firstName} ${s.lastName}`,
            admissionNo: s.admissionNo,
            status: 'PRESENT',
          }));
          this.cdr.markForCheck();
        },
        error: () => {
          this.students = [];
          this.rows = [];
          this.cdr.markForCheck();
        },
      });
  }

  onSave(): void {
    const sectionId = this.sectionControl.value;
    const academicYearId = this.currentAcademicYear?.id;
    const date = this.dateControl.value;
    if (!sectionId || !academicYearId || this.rows.length === 0) {
      return;
    }
    this.saving = true;
    const payload = {
      sectionId,
      academicYearId,
      attendanceDate: date,
      marks: this.rows.map((r) => ({ studentId: r.studentId, status: r.status })),
    };
    this.http.post<ApiResponse<unknown>>('/api/attendance/mark', payload).subscribe({
      next: () => {
        this.saving = false;
        this.cdr.markForCheck();
        alert('Attendance saved');
      },
      error: () => {
        this.saving = false;
        this.cdr.markForCheck();
        alert('Could not save attendance. Please try again.');
      },
    });
  }
}
