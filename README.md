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
hidden.

## Disclaimer

This is an independent, non-commercial project and is not
affiliated with Blizzard Entertainment.
