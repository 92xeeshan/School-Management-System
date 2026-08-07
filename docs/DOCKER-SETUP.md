# School Management System - Run with Docker

## 1. Why are we using Docker? (simple explanation)

Docker packages an application together with everything it needs (runtime,
libraries, database, configuration) into a **container**. A container is a
lightweight, isolated "mini-computer" that runs the same way on any machine.

For this project Docker gives three big benefits:

1. **No manual installation.** PostgreSQL, Redis, MinIO, the backend and
   the frontend are started with a single command. You do not have to
   install Java, Node.js, PostgreSQL or Redis on your own computer.
2. **Everything in one place.** The whole system (database + backend +
   frontend + file storage) is defined in one file,
   `docker-compose.yml`, and starts together in the correct order.
3. **Same everywhere.** Because the setup is the same on every machine, a
   "works on my machine" problem disappears: it runs identically on Windows,
   Linux and macOS.

Think of Docker as an automatic, pre-packaged way to run our whole
application without configuring anything yourself.

### What does each container do?

| Container             | Image              | What it runs                 | Port |
|-----------------------|--------------------|------------------------------|------|
| `schoolms-postgres`   | `postgres:15-alpine` | Database                   | 5432 |
| `schoolms-redis`      | `redis:7-alpine`   | Session / permission cache   | 6379 |
| `schoolms-minio`      | `minio/minio`      | File storage (PDFs, photos)  | 9000 + 9001 (console) |
| `schoolms-backend`    | built from `backend/Dockerfile` | Spring Boot REST API | 8080 |
| `schoolms-frontend`   | built from `frontend/Dockerfile` | Angular app served by nginx | 4200 |

---

## 2. Step-by-step guide

### Step 1: Install Docker

- **Windows:** install **Docker Desktop** from https://www.docker.com/products/docker-desktop/
  (it includes Docker Compose). Start Docker Desktop and keep it running.
- **Linux (Ubuntu/Debian example):**
  ```bash
  sudo apt-get update
  sudo apt-get install -y docker.io docker-compose-v2
  sudo systemctl enable --now docker
  sudo usermod -aG docker "$USER"
  ```
  Log out and back in so the `docker` group applies.
- **macOS:** install Docker Desktop like on Windows.

Verify the installation:

```bash
docker --version
docker compose version
```

### Step 2: Get the project

Clone the repository (or use the folder you already have):

```bash
git clone https://github.com/92xeeshan/School-Management-System.git
cd School-Management-System
```

### Step 3: Create the environment file (optional)

Copy the example configuration. All values have safe defaults, so this
step is optional for local testing:

```bash
cp .env.example .env
```

`.env.example` contains:

```
POSTGRES_DB=schoolms
POSTGRES_USER=schoolms
POSTGRES_PASSWORD=schoolms
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin
JWT_SECRET=change-me-in-prod-please-32chars-min
```

> For anything more than a local demo, change `POSTGRES_PASSWORD` and
> `JWT_SECRET` (must be 32+ characters) to strong values.

### Step 4: Build and start the services

From the project root, run:

```bash
docker compose up --build
```

What happens:

1. Docker downloads the base images (`postgres`, `redis`, `minio`,
   `node`, `nginx`, `maven`, ...). This happens only the first time.
2. It builds the backend image (Maven compiles the Spring Boot JAR) and the
   frontend image (npm builds the Angular app).
3. It starts the containers in order: PostgreSQL and Redis first (health
   check), then MinIO, then the backend, then the frontend.
4. On first startup the backend runs the Flyway migrations, which create
   all tables and seed the demo accounts automatically.

You will see live logs from all containers in the terminal.

### Step 5: Open the application

| Service  | URL                         |
|----------|-----------------------------|
| Frontend (UI) | http://localhost:4200 |
| Backend API   | http://localhost:8080 |
| Swagger UI    | http://localhost:8080/swagger-ui.html |
| MinIO console | http://localhost:9001  (login `minioadmin` / `minioadmin`) |

Log in with any demo account (password `Admin@123`): `superadmin`, `admin`,
`teacher`, `parent`, `student`.

### Step 6: Stop the application

- Press `Ctrl+C` in the terminal where `docker compose up` is running, **or**
- Run from another terminal:
  ```bash
  docker compose down
  ```

To stop **and delete all database data** (fresh start next time):

```bash
docker compose down -v
```

### Step 7: Useful Docker commands

| Task                            | Command                                        |
|---------------------------------|------------------------------------------------|
| Start in background (detached)  | `docker compose up -d --build`                |
| See what is running             | `docker compose ps`                            |
| Watch logs of one service       | `docker compose logs -f backend`               |
| Open a shell inside a container | `docker compose exec postgres psql -U schoolms -d schoolms` |
| Rebuild after code changes      | `docker compose up -d --build`                 |
| Remove containers               | `docker compose down`                          |
| Remove containers and data      | `docker compose down -v`                       |
| Free disk space (old images)    | `docker system prune -a`                       |

---

## 3. Troubleshooting

| Problem                                    | Solution                                                |
|--------------------------------------------|---------------------------------------------------------|
| `docker: command not found`                | Docker is not installed or not on `PATH` (Step 1)       |
| Port 5432/8080/4200 already in use         | Stop the program using the port, or change the left side of `ports:` in `docker-compose.yml` |
| `docker compose` unknown command           | Install the compose plugin (Step 1)                     |
| Frontend loads but login fails             | Wait for the backend to finish starting and migrating (`docker compose logs -f backend`) |
| Database changes needed                    | `docker compose exec postgres psql -U schoolms -d schoolms` |
| Slow first build                           | Normal: the first build downloads images and dependencies; later builds are much faster |
