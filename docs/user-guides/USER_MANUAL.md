# Role-Based User Manual

**App URL (local):** http://localhost:4200
**Demo password for all seeded logins:** `Admin@123`

| Username | Role | Notes |
|---|---|---|
| `admin` | School administrator | Full school operations |
| `teacher` | Teacher | Asha Sharma, EMP001, Class 5-A class teacher |
| `parent` | Parent | Rajesh Kumar, child Aarav ADM0001 |
| `student` | Student | Aarav Kumar ADM0001 |
| `superadmin` | Platform | All schools; use only if you manage tenants |

There is no Principal login. Admin performs certificate approval.

---

## 1. Getting Started and Account Setup

### Sign in

1. Open the app. If you are not signed in you land on `/login`.
2. Choose English, हिन्दी, or اردو (Urdu is right-to-left).
3. Enter username and password. Click **Sign in**.
4. You are taken to **Dashboard**.

If you see invalid credentials, check caps lock. If you see an inactive or transfer-certificate message, the office has closed the account.

### Session

Stay signed in on this browser. After 15 minutes the app refreshes the session in the background. If you are sent back to login, sign in again.

### Top bar

- School name / brand
- Notification bell (unread count)
- Language
- Sun/moon theme
- Profile menu (your name and role) → **Logout**

### Sidebar

Items appear only if your account has permission. Collapse the sidebar with the chevron (desktop). On a phone, use the menu button.

### Profile and password

Password recovery is **not** self-service in this version. Ask an administrator to reset your account. Locale is saved with your user when you change language.

---

## 2. Feature Walkthroughs

### 2.1 All roles — Dashboard

**Path:** Dashboard

- Summary cards for your role (counts, attendance, fees if allowed)
- Mini calendar and notices
- Quick actions (Mark attendance, Add student, and so on) only if you are allowed

### 2.2 Teacher

**Attendance:** Attendance → pick class/section/date → set Present / Absent / Late / Leave → Save.

**Students:** Students list is read-only unless you are class teacher with profile edit. Open a row for the profile.

**Examinations:** Examinations → Marks (enter/save/submit). Admit cards and report cards if published. You cannot publish marksheets.

**Certificates:** You may generate certificates and **review** requests for your class. You cannot give final approval.

**Calendar / Notices:** Read school events and notices.

### 2.3 Parent

**Attendance / Fees:** See your child’s history, installments, and receipts. You cannot mark attendance or record payments.

**Downloads:** Marksheet and certificates after the school publishes/issues them. Request a certificate under Downloads → Certificates (type, reason, optional file).

**Notices / Calendar / Dashboard:** Read-only.

Parents do not get admit-card menu access.

### 2.4 Student

**Timetable:** Academics → Timetable (if shown) or dashboard agenda.

**Attendance / Notices / Calendar:** Your own data.

**Downloads:** Admit/report/marksheet when published. Request certificates the same way as parents.

**Examinations:** Admit cards / report cards routes if your permissions include them.

### 2.5 Admin (day-to-day school)

Use this as the office playbook. Details for configuration are in `ADMIN_GUIDE.md`.

| Task | Path |
|---|---|
| Enroll a student | Students → Add |
| Edit profile | Students → row → profile |
| Staff | Staff Management → Teachers / Non-teaching |
| Classes and timetable | Academics |
| Exam timetable | Examinations → Schedules |
| Collect a fee | Fees → select student → Collect |
| Publish a notice | Notices → compose → Published |
| Calendar event | Calendar → add (if you have manage) |
| Approve results | Downloads → Marksheet → approve / publish |
| Approve certificates | Certificates → request queue → approve |

Print/PDF for admit cards, report cards, and marksheets only works when the record is **published**. Print layout is white A4.

---

## 3. Troubleshooting and FAQs

| Problem | What to try |
|---|---|
| Login error | Backend must be running. Demo password is `Admin@123`. |
| Blank page after login | Hard refresh. Clear site data for `schoolms.auth` if the session is corrupt. |
| Menu item missing | Your role does not include that permission. This is expected for students/parents. |
| Cannot print admit card | Schedule/cards not published yet. |
| Certificate request rejected immediately | A request of that type is already pending. |
| Language mixed | Switch language again; Urdu should flip the layout. |
| Dark theme on a printed page | Official print sheets stay white; only the app chrome follows theme. |
| Forgot password | Contact the school admin. |
| “Access denied” | You opened a URL your role cannot use; go to Dashboard. |

Still stuck: ask the administrator to check that your user is `ACTIVE` and has the intended role.
