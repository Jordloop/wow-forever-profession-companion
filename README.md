# WoW Forever Profession Companion

WoW Forever Profession Companion is an application in development
for profession progression and auction house analysis.

It combines profession reference data, market prices, character
skills, and inventory information to support informed decisions
about profession leveling and material profitability.

## Planned Features

- Profession progression plans based on speed, economy, or a balance of both
- Profession training and gathering requirements
- Current auction house prices and seven-day price history
- Processing profitability and material opportunity cost calculations
- Market opportunity discovery
- Multiple character profiles with individual profession and inventory tracking
- Persistent manual profession reference data

Initial support will cover Mining (including Smelting) and
Herbalism on the US Normal Alliance market.

## Technology Stack

- Java 25 and Spring Boot
- Angular, TypeScript, and SCSS
- PostgreSQL and Flyway
- Maven
- Docker Compose
- GitHub Actions

## Project Status

Application development is underway. The initial development
milestone focuses on application infrastructure, persistence,
logging, diagnostics, security, and automated testing.

Installation and configuration instructions will be added as
the application becomes operational.

## Development diagnostics

The backend writes readable console logs and structured JSON file logs.
From the `backend` directory, the default file is
`logs/profession-companion.log`; it rotates at 10 MB, keeps up to seven
days, and caps archived logs at 80 MB. Combined storage stays near 100 MB,
including the active file and a possible final rollover chunk. Override `LOG_FILE`,
`LOG_MAX_FILE_SIZE`, `LOG_MAX_HISTORY`, or `LOG_TOTAL_SIZE_CAP` to configure
the file output and retention. Use `APP_LOG_LEVEL` for application-wide
verbosity or standard `LOGGING_LEVEL_COM_WOWCOMPANION_<PACKAGE>` environment
variables for individual package levels.

The `prod` Spring profile writes structured JSON to the console as well as
the file. API responses include a server-generated `X-Correlation-ID`; the
same ID is included in application log context. Health, build information,
and metrics are served on loopback port 8082 under `/actuator/health`,
`/actuator/info`, and `/actuator/metrics`. Health component details are
hidden. API failures use RFC 9457 Problem Details with a correlation ID;
validation responses omit rejected values, and unexpected errors return
sanitized details. Unexpected-error logs retain exception types and stack
locations without logging exception messages or response bodies. The frontend
displays safe guidance and a diagnostic ID; client-generated IDs use a
`client-` prefix to distinguish them from server correlation IDs.

## Request security

The backend permits unauthenticated access for the approved personal,
localhost-only MVP; it does not add login or user management. State-changing
requests require a valid CSRF token and an allowed `Origin`. The backend issues
the Angular-readable `XSRF-TOKEN` cookie with path `/` and `SameSite=Strict`;
Angular's `HttpClient` sends it as `X-XSRF-TOKEN` for same-origin relative API
requests. Security failures return sanitized RFC 9457 responses with a
correlation ID.

Development allows only the configured Angular dev-server origins
`http://localhost:4200` and `http://127.0.0.1:4200` in addition to the request's
own origin. Every request, including safe reads, must also use an explicitly
trusted `Host` (`localhost` or a canonical loopback address); an `Origin` header
cannot make an untrusted host acceptable. The `prod` profile removes the
development-origin exceptions and accepts only the request's same origin. Do
not enable wildcard CORS or add cross-origin production access. The approved
production deployment serves the Angular assets and API from the same origin;
keep its published app and database ports loopback-only.

There are no production write endpoints yet. The security integration tests
use test-only routes. Future API reads should establish the CSRF cookie before
the first write; keep writes on unsafe HTTP methods and use Angular's
`HttpClient` with relative URLs so its XSRF support applies. Do not disable CSRF
for API routes.

## Browser tests

From `frontend`, install the Chromium browser once and run the unit and
Playwright end-to-end suites:

```powershell
npm ci --ignore-scripts
npx playwright install chromium
npm test
npm run test:e2e
```

The end-to-end suite requires Docker Engine and Compose v2. It starts an
isolated test-only database and backend with no persistent volumes, plus an
Angular development server. Playwright tests both the production-Compose
origin and the development-proxy origin, including real browser CSRF cookie and
header exchange and invalid-token rejection. Shell checks cover workspace
navigation, direct route loads, keyboard skip navigation, responsive layout and
the current disabled placeholder controls. The test routes are enabled only in
the browser-test profile; the suite uses no provider or live account data.

## Run with Docker Compose

Docker Engine and Docker Compose v2 are required. If `.env` does not already
exist, create it from `.env.example`, then replace the database password
placeholder with a locally generated password. Keep `.env` private and do not
overwrite an existing `.env` with the example file.

From the repository root, start the production-profile application and database:

```powershell
docker compose up -d --build --wait
```

Open `http://127.0.0.1:8083`. The app port is published only on loopback; change
`APP_PORT` in `.env` if that host port is already in use. PostgreSQL has no
published host port. Spring serves both the Angular application and backend
from the same origin. The production container runs as a non-root user and
waits for the database health check before starting.

Check service health with `docker compose ps` and follow structured application
logs with `docker compose logs -f app`. The database data is stored in the
`postgres-data` named volume; rotating application files are stored in the
`application-logs` named volume. Rebuilding/replacing containers preserves
these volumes. `docker compose down` preserves them; do not use `docker compose
down -v` for normal updates because it deletes persistent data and logs.

To update after obtaining newer source, back up the database before applying a
release that may include destructive schema changes, then run
`docker compose up -d --build --wait`. Flyway applies migrations during app
startup. Compose keeps the named database volume across app image updates.

### Database backup and restore

Backups contain local application data and are ignored by Git. Create a
PostgreSQL custom-format backup from PowerShell:

```powershell
New-Item -ItemType Directory -Force .\backups | Out-Null
$backup = "backups\wowcompanion-$(Get-Date -Format yyyyMMdd-HHmmss).dump"
cmd.exe /d /c "docker compose exec -T db pg_dump -Fc -U wowcompanion wowcompanion > $backup"
if ($LASTEXITCODE -ne 0) { throw "Database backup failed." }
```

Validate a backup before relying on it:

```powershell
cmd.exe /d /c "docker compose exec -T db pg_restore --list < $backup"
if ($LASTEXITCODE -ne 0) { throw "Database backup validation failed." }
```

Restoring with `pg_restore --clean` replaces objects in the target database and
is destructive. First make and validate a separate backup of the current
database, stop the app, and verify the target and backup paths. Only then run:

```powershell
cmd.exe /d /c "docker compose exec -T db pg_restore --clean --if-exists --exit-on-error --single-transaction -U wowcompanion -d wowcompanion < $backup"
if ($LASTEXITCODE -ne 0) { throw "Database restore failed." }
docker compose up -d --wait
```

Do not delete the named database volume as part of routine recovery or updates.

## Disclaimer

This is an independent, non-commercial project and is not
affiliated with Blizzard Entertainment.
