# School Management System - Frontend (Angular)

Single-page application for the School Management System. It talks to the
Spring Boot backend through a relative `/api` prefix, so no API base URL
configuration is needed.

**Stack:** Angular 19 (standalone components, signals, reactive forms),
Angular Material theming, ngx-translate (English / Hindi / Urdu with RTL).

---

## 1. Required software to install locally

| Software   | Version     | Purpose                              | Download                          |
|------------|-------------|--------------------------------------|-----------------------------------|
| Node.js    | 20 or newer | Run the Angular dev server and build | https://nodejs.org (LTS 20/22)    |
| npm        | 10+         | Comes with Node.js                    | included with Node.js             |
| Git        | Latest      | Clone the repository                 | https://git-scm.com/downloads     |

> Angular CLI is optional: this project uses the local `node_modules`
> binaries, so you never need a global `ng` install.

> The frontend needs the **backend running on port 8080** (see
> `docs/README-BACKEND.md`). If you only start the frontend you will reach
> the login page but cannot authenticate.

---

## 2. Required settings on your local system

### 2.1 API proxy (already configured)

`src/proxy.conf.json` forwards every `/api` request to the backend:

```json
{
  "/api": {
    "target": "http://localhost:8080",
    "changeOrigin": true,
    "secure": false
  }
}
```

The Angular dev server loads this file automatically (`angular.json` ->
`serve.options.proxyConfig`). Change `target` only if your backend runs on
another port/host.

### 2.2 Allowed hosts (already configured)

`angular.json` already whitelists `*.monkeycode-ai.live` for the online
preview. On a local machine you normally access the dev server through
`localhost`, which is always allowed.

### 2.3 Windows vs Linux

There are **no platform-specific settings** for the frontend. Steps 2.1 and
2.2 work identically on Windows and Linux. Only make sure Node.js and npm
are on your `PATH`.

---

## 3. How to run the application

### 3.1 Install dependencies

```bash
cd frontend
npm install
```

### 3.2 Start the development server

```bash
npm start
```

Then open http://localhost:4200 in your browser. The dev server proxies
`/api` to the backend on port 8080. Hot reload is enabled.

### 3.3 One-click script (Windows / Linux)

Use the provided launcher that starts the frontend and backend together:

- Linux/macOS: `./scripts/run-linux.sh`
- Windows: double-click `scripts\run-windows.bat`

### 3.4 Other commands

| Task                | Command                         | Output                          |
|---------------------|---------------------------------|---------------------------------|
| Production build    | `npm run build`                 | `frontend/dist/frontend/browser`|
| Unit tests (Karma)  | `npm test`                      | Karma runner in Chrome          |
| Serve built app     | `npx http-server dist/frontend/browser -p 4200` | static preview     |

### 3.5 Demo accounts

Open http://localhost:4200 and sign in with any account from the backend
seed data. Password for all accounts is `Admin@123`:

| Username     | Role    | What you can do                                  |
|--------------|---------|--------------------------------------------------|
| `admin`      | Admin   | Full school management                           |
| `teacher`    | Teacher | Mark/read attendance, view timetable             |
| `parent`     | Parent  | View child attendance and fees                   |
| `student`    | Student | View own timetable, attendance, notices          |
| `superadmin` | Super Admin | Platform-level (schools/users) access      |

---

## 4. Troubleshooting

| Problem                        | Solution                                                |
|--------------------------------|---------------------------------------------------------|
| `npm install` fails on network | Retry, or set a local npm registry mirror               |
| Login always shows an error    | Make sure the backend is running on port 8080 (section 3) |
| 403 on every API call          | Frontend origin not in backend `CORS_ALLOWED_ORIGINS`   |
| Port 4200 already in use       | Stop the other process, or serve on another port with `ng serve --port 4201` (then update the proxy target) |
| API requests 404               | Backend not started / proxy target wrong in `proxy.conf.json` |
