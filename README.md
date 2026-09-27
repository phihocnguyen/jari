# Jari

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-green)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)](https://www.postgresql.org/)
[![Frontend](https://img.shields.io/badge/frontend-jari--client-000?logo=next.js&logoColor=white)](https://github.com/phihocnguyen/jari-client)

```
     ██╗ █████╗ ██████╗ ██╗
     ██║██╔══██╗██╔══██╗██║
     ██║███████║██████╔╝██║
██   ██║██╔══██║██╔══██╗██║
╚█████╔╝██║  ██║██║  ██║██║
 ╚════╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝
```

**Open-source issue tracking platform — backend API.**

Jari is a self-hosted, Jira-inspired project management backend. It exposes a REST API for workspaces, projects, issues, sprints, boards, and notifications, with Redis caching, Elasticsearch-powered search, real-time WebSocket updates, and a full observability stack for production-style benchmarking.

**Frontend:** [jari-client](https://github.com/phihocnguyen/jari-client) (Next.js)

**Support:** macOS, Linux (primary dev targets)

---

## Features

- **Workspaces & projects** — multi-tenant hierarchy with RBAC, members, and project settings
- **Issues** — create, filter, assign, label, link to components/releases, full history and comments
- **Agile boards** — Kanban board, backlog, active sprint management, drag-and-drop ordering
- **Dashboard summary** — status/priority/type breakdowns, team workload, recent activity, epic progress
- **Search** — Elasticsearch index for fast issue filtering; PostgreSQL fallback when ES is unavailable
- **Caching** — Redis-backed `@Cacheable` on read-heavy endpoints (ref data, issue list/detail, board, summary) with Micrometer hit/miss metrics
- **Real-time** — STOMP WebSocket for notifications and project presence
- **Auth** — JWT access/refresh tokens, optional Google OAuth2
- **Automation** — rule hooks on issue status changes with audit logs
- **Development** — link branches/commits/PRs to issues; GitHub App auto-links by issue key
- **Observability** — Prometheus metrics, Grafana dashboards (API latency + cache hit rate), ELK log pipeline
- **Load testing** — k6 scenarios for smoke, stress, and capacity discovery

---

## Architecture

```mermaid
flowchart LR
  subgraph client [jari-client]
    FE[Next.js :3000]
  end

  subgraph backend [jari backend]
    API[Spring Boot :8080]
    WS[WebSocket /ws]
    Dev[development.github]
  end

  subgraph data [Data layer]
    PG[(PostgreSQL)]
    RD[(Redis)]
    ES[(Elasticsearch)]
    MQ[(RabbitMQ)]
  end

  subgraph githubCloud [GitHub]
    GHApp[GitHub App]
  end

  subgraph observability [Monitoring]
    PR[Prometheus]
    GF[Grafana]
    KB[Kibana]
  end

  FE -->|REST /api/v1| API
  FE -->|STOMP| WS
  GHApp -->|webhooks| Dev
  Dev --> API
  API --> PG
  API --> RD
  API --> ES
  API --> MQ
  API -->|/actuator/prometheus| PR
  PR --> GF
  API -.->|JSON logs| KB
```

| Layer      | Technology                                             |
| ---------- | ------------------------------------------------------ |
| API        | Spring Boot 4.1, Java 21 (virtual threads), Spring Security, MapStruct |
| Database   | PostgreSQL 16, Flyway migrations, HikariCP             |
| Cache      | Redis 7, Jackson JSON serialization                    |
| Search     | Elasticsearch (Spring Data ES + Logstash JDBC indexer) |
| Messaging  | RabbitMQ (async notifications)                         |
| Metrics    | Micrometer, Prometheus, Grafana                        |
| Logs       | Logstash, Filebeat, Kibana (optional)                  |
| Load tests | k6                                                     |

---

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- Node.js 20+ (for frontend — see [jari-client](https://github.com/phihocnguyen/jari-client))

### 1. Start infrastructure

```bash
git clone https://github.com/phihocnguyen/jari.git
cd jari

cp .env.example .env   # optional — defaults work for local dev
docker compose up -d
```

This starts **PostgreSQL**, **Redis**, and **RabbitMQ**.

| Service    | URL / Port                                                 |
| ---------- | ---------------------------------------------------------- |
| PostgreSQL | `localhost:5432` — db/user/pass: `jari`                    |
| Redis      | `localhost:6379`                                           |
| RabbitMQ   | AMQP `5672`, UI http://localhost:15672 (`guest` / `guest`) |

### 2. Start the backend

```bash
./mvnw spring-boot:run
```

| Endpoint   | URL                                       |
| ---------- | ----------------------------------------- |
| API base   | http://localhost:8080/api/v1              |
| Swagger UI | http://localhost:8080/swagger-ui.html     |
| Health     | http://localhost:8080/actuator/health     |
| Prometheus | http://localhost:8080/actuator/prometheus |

On first run with Spring profile `dev` (`SPRING_PROFILES_ACTIVE=dev`), **DevDataInitializer** seeds a workspace (`ACME`) and project (`TIS`). Register or log in via `/api/v1/auth` before calling protected APIs.

### 3. Link the frontend (jari-client)

The frontend lives in a **separate repository** and talks to this backend over HTTP and WebSocket.

```bash
git clone https://github.com/phihocnguyen/jari-client.git
cd jari-client
npm install
```

Create `.env.local` in the frontend root:

```bash
# REST API (must include /api/v1)
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1

# WebSocket (STOMP over SockJS)
NEXT_PUBLIC_WS_URL=http://localhost:8080/ws

# Use real API (set to true only for offline demo mode)
NEXT_PUBLIC_USE_MOCK=false
```

Start the frontend:

```bash
npm run dev
```

Open http://localhost:3000

**CORS** — the backend allows `http://localhost:3000` by default (`CORS_ORIGINS` in `.env` or `application.yaml`). For a custom frontend port:

```bash
export CORS_ORIGINS=http://localhost:3000,http://localhost:3001
```

**Auth** — APIs require a JWT (except `/api/v1/auth/**`, OAuth2, actuator health, and GitHub webhooks). Register/login to obtain tokens, or use Google OAuth when enabled.

**OAuth2 Google** — set `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` and `app.security.oauth2-enabled=true`; the OAuth callback redirects to `http://localhost:3000/auth/callback`.

---

## Development panel & GitHub App

Issue **Development** links (branch / commit / pull request) live in `issue_developments` and are exposed via:

| Method | Path | Purpose |
| ------ | ---- | ------- |
| `GET` | `/api/v1/issues/{id}/developments` | List links on an issue |
| `POST` | `/api/v1/issues/{id}/developments` | Manual link |
| `DELETE` | `/api/v1/developments/{id}` | Unlink |

GitHub App automation lives under `com.example.jari.development.github` and fills the same table when a mapped repo emits events whose title, branch, or commit message contains an issue key (e.g. `APP-1`).

```mermaid
sequenceDiagram
  participant Admin
  participant FE as jari-client
  participant API as jari
  participant GH as GitHub

  Admin->>FE: Settings Integrations Connect
  FE->>API: GET install-url
  API-->>FE: GitHub App install URL + signed state
  Admin->>GH: Install App select repos
  GH->>API: GET /api/v1/github/setup
  API->>API: Link installation to workspace sync repos
  Admin->>FE: Map repo to Jari project
  Note over GH,API: Runtime
  GH->>API: POST /api/v1/webhooks/github
  API->>API: Verify HMAC parse keys upsert IssueDevelopment
```

### Setup

1. Create a GitHub App with permissions: **Contents** (Read), **Metadata** (Read), **Pull requests** (Read).  
   Subscribe to events: `Installation`, `Installation repositories`, `Push`, `Create`, `Pull request`.
2. **Webhook URL:** `https://<public-host>/api/v1/webhooks/github` (ngrok / Cloudflare Tunnel locally).
3. **Setup URL:** `https://<public-host>/api/v1/github/setup`
4. Env vars: `GITHUB_APP_ID`, `GITHUB_APP_SLUG`, `GITHUB_APP_PRIVATE_KEY`, `GITHUB_WEBHOOK_SECRET`, `GITHUB_APP_CLIENT_ID`, `GITHUB_APP_CLIENT_SECRET`, `FRONTEND_BASE_URL`.
5. In the UI: **Workspace Settings → Integrations → Connect GitHub**, then map each repo to a project.
6. Smoke test: branch `feature/APP-1-x`, commit `APP-1 msg`, PR title `APP-1 …` → appear on ticket Development panel.

**Local tunnel (ngrok in Docker)** — expose host `:8080` without installing ngrok on the machine:

```bash
# 1. Put token in .env  (https://dashboard.ngrok.com/get-started/your-authtoken)
NGROK_AUTHTOKEN=...

# 2. Start Spring Boot on the host (port 8080), then:
docker compose --profile tunnel up -d ngrok

# 3. Copy the https URL from:
#    http://localhost:4040   or   docker compose logs ngrok
# Use it as GitHub App Webhook + Setup URL base, e.g.:
#    https://xxxx.ngrok-free.app/api/v1/webhooks/github
#    https://xxxx.ngrok-free.app/api/v1/github/setup
```

Also set `BACKEND_BASE_URL` to that https URL if you rely on it in docs/callbacks.

### Admin APIs

| Method | Path | Purpose |
| ------ | ---- | ------- |
| `GET` | `/api/v1/workspaces/{id}/github/install-url` | Start App install |
| `GET` | `/api/v1/workspaces/{id}/github/installations` | List installs + repos |
| `PUT` | `/api/v1/workspaces/{id}/github/repos/{repoId}` | Map/unmap `{ "projectId": "…" }` |
| `DELETE` | `/api/v1/workspaces/{id}/github/installations/{id}` | Disconnect install |
| `POST` | `/api/v1/webhooks/github` | GitHub webhook (HMAC, public) |
| `GET` | `/api/v1/github/setup` | Post-install redirect callback |

Webhook events → development types: `create` (branch) → `BRANCH`, `push` → `COMMIT`, `pull_request` → `PULL_REQUEST` (status `OPEN` / `MERGED` / `CLOSED`).

---

## Configuration

Copy and adjust environment variables:

```bash
cp .env.example .env
```

| Variable                    | Default                                 | Description                                |
| --------------------------- | --------------------------------------- | ------------------------------------------ |
| `DB_URL`                    | `jdbc:postgresql://localhost:5432/jari` | PostgreSQL JDBC URL                        |
| `DB_USER` / `DB_PASS`       | `jari`                                  | Database credentials                       |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379`                    | Redis cache                                |
| `ELASTICSEARCH_URIS`        | `http://localhost:9200`                 | ES for issue search                        |
| `RABBITMQ_*`                | `guest` / `guest`                       | Message broker                             |
| `JWT_SECRET`                | dev default                             | JWT signing key — **change in production** |
| `CORS_ORIGINS`              | `http://localhost:3000`                 | Allowed frontend origins                   |
| `SPRING_PROFILES_ACTIVE`    | —                                       | Set `docker` for JSON logs → ELK           |
| `GITHUB_APP_ID`             | —                                       | GitHub App ID                              |
| `GITHUB_APP_CLIENT_ID`      | —                                       | GitHub App client ID                       |
| `GITHUB_APP_CLIENT_SECRET`  | —                                       | GitHub App client secret                   |
| `GITHUB_APP_PRIVATE_KEY`    | —                                       | PEM private key (`\n` escaped OK)          |
| `GITHUB_WEBHOOK_SECRET`     | —                                       | Webhook HMAC secret                        |
| `GITHUB_APP_SLUG`           | —                                       | App slug for install URL                   |
| `FRONTEND_BASE_URL`         | `http://localhost:3000`                 | Post-install redirect target               |
| `BACKEND_BASE_URL`          | `http://localhost:8080`                 | Public API base (docs / tunnels)           |

Key settings in `src/main/resources/application.yaml`:

```yaml
app:
  security:
    oauth2-enabled: true

spring:
  cache:
    redis:
      time-to-live: 300000 # 5 minutes
      enable-statistics: true # cache hit/miss metrics
```

---

## API Overview

All routes are under `/api/v1` unless noted.

| Area          | Examples                                                        |
| ------------- | --------------------------------------------------------------- |
| Auth          | `POST /auth/login`, `POST /auth/register`, `POST /auth/refresh` |
| Workspaces    | `GET/POST /workspaces`, members, settings                       |
| Projects      | `GET/POST /workspaces/{id}/projects`, members                   |
| Issues        | CRUD, filters, labels, watchers, history, comments              |
| Sprints       | planning, active sprint, board, backlog reorder                 |
| Summary       | `GET /projects/{id}/summary`                                    |
| Reference     | `GET /ref/issue-types`, `/ref/statuses`, `/ref/priorities`      |
| Notifications | inbox, mark read                                                |
| Development   | issue developments CRUD; GitHub App install, map, webhooks      |
| WebSocket     | `/ws` — STOMP topics for notifications & presence               |

Interactive docs: http://localhost:8080/swagger-ui.html

---

## Caching

Read-heavy endpoints use Spring Cache backed by Redis (TTL 5 minutes by default):

| Cache name        | Endpoint pattern                                   |
| ----------------- | -------------------------------------------------- |
| `ref:*`           | Reference data (issue types, statuses, priorities) |
| `issue:list`      | Paginated issue list                               |
| `issue:detail`    | Single issue by ID or key                          |
| `project:board`   | Active sprint board                                |
| `project:summary` | Project dashboard metrics                          |

### Invalidation (write path)

On issue create/update/delete, `ReadCacheEviction` invalidates:

- `issue:detail` for that issue (by id and key)
- `project:summary` for the project
- `project:board` **only if** the issue is on an **ACTIVE** sprint (backlog-only writes skip board eviction)

`issue:list` is **not** cleared on every write (avoids global cache storms). List entries expire via Redis TTL.

Board responses omit description / label / component bags so rebuilds stay smaller when the cache does miss.

Cache hit rate is exposed via Micrometer (`cache_gets_total{result="hit|miss"}`) and visualized in Grafana.

After changing cache serialization or DTOs, flush Redis before retesting:

```bash
redis-cli FLUSHDB
```

---

## Performance notes (write-heavy)

Optimizations aimed at high-concurrency writes on a single node:

| Area | Change |
| ---- | ------ |
| Auth | Hot path is **JWT verify only** — no per-request `UserDetails` DB load |
| Runtime | Java 21 **virtual threads** (`spring.threads.virtual.enabled=true`) |
| Issue keys | Per-project counter via `UPDATE … RETURNING` in a **short separate TX**, then insert TX (avoids holding the counter-row lock across the full create) |
| Project create | `saveAndFlush` before inserting `project_issue_counter` (same TX) so FK does not fail |
| Indexing | `IssueIndexEvent` published **inside** the write TX → RabbitMQ after commit → ES async (not on the request thread) |
| Board cache | Slim DTO on board; conditional board eviction (see Caching) |
| List cache | No full `issue:list` clear on every write |

Hikari pool defaults to `HIKARI_MAX_POOL_SIZE=100` (keep below Postgres `max_connections`). Virtual threads do **not** increase DB capacity — sustained concurrent DB work is still bounded by the pool.

---

## Load Testing (k6)

Benchmark scripts live in `k6/`.

```bash
cp k6/.env.example k6/.env
cd k6

./run.sh benchmark.js   # smoke + read dashboard + mixed workload
./run.sh load.js        # fixed stress — read dashboard (configure VUs / RPS in k6/.env)
./run.sh write-load.js  # field-update heavy on a small seed pool
./run.sh capacity.js    # stepped capacity discovery
```

### `write-load.js` (recommended local write baseline)

Designed to measure **status / field update latency**, not board growth:

| Setting | Typical local value | Meaning |
| ------- | ------------------- | ------- |
| `K6_CONCURRENT_USERS` | `100` | Align roughly with Hikari pool size |
| `K6_THINK_MS` | `300` | Think time between iterations |
| `K6_SEED_ISSUES` | `5` | Fixed hot issue pool |
| `K6_LOAD_DURATION` | `5m` | Steady state after 1m ramp |

Workload mix (~):

- **10%** create — **backlog only** (no `sprintId`)
- **70%** update — status / title+description / priority on the **seed pool only**
- **20%** read — mostly issue detail + list; board only occasionally

Setup always creates an **ephemeral workspace + project**. Teardown deletes the workspace (DB `CASCADE` removes issues/sprints). Set `K6_SKIP_CLEANUP=true` to keep data for debugging.

```bash
# k6/.env example
BASE_URL=http://localhost:8080
AUTH_EMAIL=user@example.com
AUTH_PASSWORD=Password123@
K6_CONCURRENT_USERS=100
K6_THINK_MS=300
K6_LOAD_DURATION=5m
K6_SEED_ISSUES=5
K6_MAX_VUS=150
```

```bash
cd k6 && ./run.sh write-load.js
```

Report **write** and **read** p95 separately (`workload:write` / `workload:read`). Do not compare write-heavy numbers to read-only `load.js` (dashboard GETs with high cache hit rate).

**Do not** attach most creates to the active sprint in this scenario — that grows the board without bound, inflates `data_received`, and makes read p95 look like a multi-second failure even when updates are fine.

### Local baseline (indicative)

Single-node laptop run of `write-load.js` with the settings above (100 VUs, 5 seed backlog issues, ~300ms think time, ~6.5m including ramp). **Not** a production SLA — hardware and mix will change results.

| Metric | Value |
| ------ | ----- |
| Checks / errors | 100% checks, **0%** `http_req_failed` |
| Throughput | ~224 req/s · ~22 create/s |
| Overall | med ~9ms · **p95 ~140ms** · p99 ~2.2s |
| `workload:write` | med ~9ms · **p95 ~232ms** · p99 ~3.0s |
| `workload:read` | med ~5ms · **p95 ~23ms** · p99 ~59ms |
| `data_received` | ~150 MB (vs multi-GB when creates were attached to the active sprint) |
| Redis cache hit rate (overall, ~5m rate during/around this mix) | typically **~10–60%** (write-invalidate heavy; not comparable to read-only `load.js` ~99%+) |

Earlier mixes that put most creates on the active sprint inflated board payloads and pushed **read p95 into multi-second** territory; that is a scenario artifact, not the update-path baseline above.

Capture hit rate while the test runs (Prometheus must scrape the app):

```bash
# overall hit ratio (0–1)
curl -sG 'http://localhost:9090/api/v1/query' \
  --data-urlencode 'query=jari:cache_hit_rate_overall:5m'
```

> Latency and throughput reflect a **single-node local** setup. Production with horizontal scaling will differ.

---

## Monitoring

Full stack in `monitoring/`:

```bash
cd monitoring
cp .env.example .env
docker compose up -d
```

| Service        | URL                   | Purpose                                                  |
| -------------- | --------------------- | -------------------------------------------------------- |
| **Grafana**    | http://localhost:3001 | API req/s, p95 latency, **Redis cache hit rate** (UTC+7) |
| **Prometheus** | http://localhost:9090 | Metrics engine (UTC)                                     |
| **Kibana**     | http://localhost:5601 | Error & request logs                                     |

See [monitoring/README.md](monitoring/README.md) for ELK setup, PromQL examples, and benchmark workflow.

---

## Project Structure

```
jari/
├── src/main/java/com/example/jari/
│   ├── issue/          # Issues, comments, history, summary, search
│   ├── sprint/         # Sprints, board, backlog
│   ├── project/        # Projects, members, presence
│   ├── workspace/      # Workspaces, RBAC
│   ├── reference/      # Issue types, statuses, priorities
│   ├── notification/   # In-app + async notifications
│   ├── automation/     # Issue automation rules
│   └── shared/         # Config, cache, security, exceptions
├── src/main/resources/
│   ├── application.yaml
│   └── db/migration/   # Flyway SQL
├── docker-compose.yml  # Postgres, Redis, RabbitMQ
├── k6/                 # Load & capacity tests
└── monitoring/         # Prometheus, Grafana, ELK
```

**Related repository:**

| Repo                                                       | Role                    |
| ---------------------------------------------------------- | ----------------------- |
| [jari](https://github.com/phihocnguyen/jari)               | Backend API (this repo) |
| [jari-client](https://github.com/phihocnguyen/jari-client) | Next.js frontend        |

---

## Development

```bash
# Run unit tests
./mvnw test

# Run tests + enforce JaCoCo branch coverage gate (≥ 70%)
./mvnw verify

# Compile only
./mvnw compile -DskipTests

# Run the API (JWT required on protected routes)
./mvnw spring-boot:run

# Optional demo seed data
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

### Testing & coverage

Unit tests use **JUnit 5 + Mockito**. Integration test (`JariApplicationTests`, Testcontainers) is tagged `@Tag("integration")` and excluded from default `./mvnw test`.

**JaCoCo report** (generated after `./mvnw test`):

|                 |                                                                  |
| --------------- | ---------------------------------------------------------------- |
| HTML report     | [`target/site/jacoco/index.html`](target/site/jacoco/index.html) |
| CSV (scripting) | `target/site/jacoco/jacoco.csv`                                  |

```bash
# Regenerate report
./mvnw test

# Open report (Linux)
xdg-open target/site/jacoco/index.html
```

**Current metrics** (unit tests only, local run):

| Metric               | All classes    | Business logic\*                          |
| -------------------- | -------------- | ----------------------------------------- |
| Unit tests           | **173** passed | —                                         |
| Branch coverage      | 39%            | **72%**                                   |
| Line coverage        | 47%            | **93%**                                   |
| Instruction coverage | 46%            | —                                         |
| CI gate              | —              | `./mvnw verify` requires **≥ 70% branch** |

\*Business logic scope: services, specs, exception handlers, cache helpers, search — excludes entity/DTO/mapper/config/controller/security, messaging consumers, GitHub App integration (`development/github`), and hard-to-unit-test services (`SummaryService`, `NotificationService`, `IssueIndexService`, `ReportService`). See `jacoco-maven-plugin` excludes in `pom.xml`.

**Typical local workflow:**

1. `docker compose up -d` — infra
2. `./mvnw spring-boot:run` — backend on `:8080`
3. `cd ../jari-client && npm run dev` — frontend on `:3000`
4. Optional: `cd monitoring && docker compose up -d` — Grafana during k6 runs

---

## Resources

- [CONTRIBUTING.md](CONTRIBUTING.md) — git / Conventional Commits conventions
- [JaCoCo coverage report](target/site/jacoco/index.html) — branch/line coverage (run `./mvnw test` first)
- [monitoring/README.md](monitoring/README.md) — Grafana, Prometheus, Kibana, cache metrics
- [k6/.env.example](k6/.env.example) — load test configuration
- [Swagger UI](http://localhost:8080/swagger-ui.html) — live API docs (when backend is running)

---

## License

See repository for license information.
