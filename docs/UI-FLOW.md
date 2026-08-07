# School Management System - UI Flow (step by step, all roles)

This document walks through the complete end-to-end user journey for every
role. The same application is used by all roles; the menus and the actions
available depend on the logged-in user's role and permissions.

The UI has six navigation items (left sidebar):

| Menu        | Route        | What it contains                               |
|-------------|--------------|------------------------------------------------|
| Dashboard   | `/dashboard` | Role-specific summary cards                    |
| Students    | `/students`  | Student list, add/edit, guardians, enrollment  |
| Academics   | `/academics` | Classes, sections, subjects, timetable         |
| Attendance  | `/attendance`| Mark and review daily attendance               |
| Fees        | `/fees`      | Fee structures, assignments, payments, receipts|
| Notices     | `/notices`   | Create, publish and read notices               |

Every page can show the menu icons only when the user's role is granted the
matching backend permission; otherwise the API returns 403 and the page
shows an error.

---

## 1. Login (common to all roles)

1. Open the app at `http://localhost:4200`. You are redirected to `/login`
   if you are not signed in (`AuthGuard`).
2. The login screen shows the app name, username and password fields, and a
   language pill switcher (English / Hindi / Urdu).
3. Enter your username and password.
   - Demo accounts (password `Admin@123`): `superadmin`, `admin`,
     `teacher`, `parent`, `student`.
4. Click **Sign in**.
   - The backend validates the credentials via `/api/auth/login` and
     returns a short-lived JWT access token plus a long-lived refresh token.
   - The frontend stores the tokens and the user profile, then redirects to
     `/dashboard` (or the `returnUrl` you were heading to).
5. From now on every API call automatically carries the access token. When
   the access token expires (15 min), the interceptor transparently calls
   `/api/auth/refresh` to get a new pair and retries the request, so you are
   not logged out.
6. The top bar shows your avatar and name, a language switcher and a
   **Logout** button. Clicking Logout calls `/api/auth/logout`, clears the
   session and returns you to `/login`.

---

## 2. Super Admin flow (platform level)

The Super Admin manages schools across the whole platform.

1. **Login** as `superadmin` (password `Admin@123`).
2. **Dashboard** – see a platform-level summary (all schools, users).
3. **Schools** – list the schools registered on the platform
   (`/api/schools`), open a school record to view its details, and edit or
   add schools (`SCHOOL_READ` / `SCHOOL_MANAGE` permissions).
4. **Users** – read users and assign roles at platform level
   (`USER_READ`, `ROLE_ASSIGN`).
5. Everything else is available too, because the Super Admin role carries
   every permission in the `role_permission` matrix.
6. **Logout** when done.

> The Super Admin belongs to no single school (`school_id` is NULL); the
> backend works in "bypass RLS" mode for platform operations.

---

## 3. Admin flow (school administrator)

The Admin runs the school day to day. This is the richest workflow.

1. **Login** as `admin`.
2. **Dashboard** – summary cards: total students, attendance today,
   pending fees, recent notices.
3. **Students** (`/students`)
   1. The list shows all students with admission number, name, class/section.
   2. Click **Add Student** – fill personal details (name, DOB, gender,
      blood group), guardian details and enrollment details (class,
      section, roll number). Saving creates the student profile, the
      guardian record and the enrollment in one call.
   3. Click a student row to view/edit the profile, change enrollment, or
      assign guardians.
   4. Bulk CSV import is available (`STUDENT_IMPORT`).
4. **Academics** (`/academics`)
   1. **Classes** – add/edit classes (`CLASS_CREATE` / `CLASS_UPDATE`).
   2. **Sections** – create sections under a class with a capacity.
   3. **Subjects** – maintain the subject catalogue.
   4. **Timetable** – define periods per section/day and assign subject +
      teacher (`TIMETABLE_MANAGE`).
5. **Attendance** (`/attendance`)
   1. Select a class/section and date.
   2. The teacher roster appears; mark each student Present / Absent /
      Late / Leave, or use "mark all present".
   3. Save – the backend creates the attendance session and records
      (`ATTENDANCE_MARK`).
   4. Review past days via the history view (`ATTENDANCE_READ`).
6. **Fees** (`/fees`)
   1. **Categories** – tuition, transport, exam, library, sports.
   2. **Structures** – define a fee structure per class, academic year and
      category with amount, frequency and due day
      (`FEE_STRUCTURE_MANAGE`).
   3. **Assignments** – assign fee structures to individual students
      (optionally with a discount).
   4. **Installments** – generated from the structures; status PENDING,
      PARTIAL, PAID, OVERDUE, WAIVED.
   5. **Record payment** – enter the amount, method (Cash/Card/UPI/Bank
      transfer) and reference number (`FEE_PAYMENT_RECORD`).
   6. **Receipt** – download the PDF receipt (`FEE_RECEIPT_VIEW`).
7. **Notices** (`/notices`)
   1. Click **New Notice** – write title/body, choose scope (school-wide or
      class-wide), priority, and publish date.
   2. Save as **Draft**, then **Publish** when ready (`NOTICE_PUBLISH`),
      or **Archive**/delete.
8. **Users** – create school users and assign roles (admin only).
9. **Language** – switch the UI language from the top bar; the choice is
   saved per user and remembered next login.
10. **Logout**.

---

## 4. Teacher flow

1. **Login** as `teacher`.
2. **Dashboard** – today's schedule, my sections, quick attendance status.
3. **Attendance** (`/attendance`)
   1. Pick one of your assigned sections and today's date.
   2. Mark each student Present / Absent / Late / Leave.
   3. Save the session. (Teacher has `ATTENDANCE_MARK` but not
      `ATTENDANCE_READ` beyond their own scope.)
4. **Timetable** – view your weekly schedule
   (`TIMETABLE_READ`).
5. **Students** – read-only access to the students in your sections
   (`STUDENT_READ`).
6. **Notices** – read school notices (`NOTICE_READ`).
7. **Logout**.

---

## 5. Parent flow

1. **Login** as `parent` (demo account `parent`).
2. **Dashboard** – overview for your child(ren): attendance percentage,
   fee balance, latest notices.
3. **Attendance** – view your child's attendance history
   (`ATTENDANCE_READ`).
4. **Fees** – view your child's fee assignments, installments and payment
   history, and download receipts (`FEE_READ`, `FEE_RECEIPT_VIEW`).
5. **Notices** – read school-wide notices (`NOTICE_READ`).
6. **Timetable** – view your child's class timetable
   (`TIMETABLE_READ`).
7. **Logout**.

---

## 6. Student flow

1. **Login** as `student`.
2. **Dashboard** – your own summary (attendance, fees due, notices).
3. **Timetable** – your class timetable (`TIMETABLE_READ`).
4. **Attendance** – see your own attendance records
   (`ATTENDANCE_READ`).
5. **Notices** – read school notices (`NOTICE_READ`).
6. **Logout**.

---

## 7. End-to-end walkthrough example (Admin)

A typical day for the school Admin, in order:

1. Open `http://localhost:4200` -> login as `admin`.
2. Dashboard shows 5 students, today's attendance progress, 3 pending
   installments, and the latest published notice.
3. Go to **Academics** -> add a new class "Class 7", then a section "A".
4. Go to **Students** -> **Add Student** and enroll the new student into
   Class 7 A.
5. Go to **Fees** -> add a tuition structure for Class 7, assign it to the
   new student.
6. Go to **Notices** -> create a welcome notice and publish it.
7. Log out.

---

## 8. Permission matrix (summary)

| Permission            | Super Admin | Admin | Teacher | Student | Parent |
|-----------------------|:-----------:|:-----:|:-------:|:-------:|:------:|
| Users (CRUD + assign) | yes         | yes   | -       | -       | -      |
| Students (CRUD/import)| yes         | yes   | read    | -       | -      |
| Academics (classes/sections/subjects) | yes | yes | read | - | - |
| Timetable             | manage      | manage | read  | read    | read   |
| Attendance            | mark/read   | mark/read | mark | read | read |
| Fees (structure/payment) | yes     | yes   | -       | -       | read   |
| Notices               | create/publish | create/publish | read | read | read |
| Dashboard             | yes         | yes   | yes     | yes     | yes    |

The matrix above is enforced on the backend through the `role_permission`
table (see `db/02_insert_records.sql`); the UI simply reflects it.
