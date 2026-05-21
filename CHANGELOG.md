# Changelog

All notable changes to **Measurement Conversion API** are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the
project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-05-21

### Added
- Initial public release of the Measurement Conversion REST API.
- `GET /api/conversion` endpoint that decodes a measurement input string into
  per-package totals using the state-based parser.
- State-based `MeasurementParser` supporting:
  - Package-mode header decoding (`a=1 .. z=26`).
  - `z` continuation chains (e.g. `zd = 30`, `zza = 53`).
  - Value-mode decoding with `_ = 0`.
- Persistent history of every request in Oracle XE.
- Full CRUD for history:
  - `GET    /api/history`        — list all
  - `GET    /api/history/{id}`   — fetch by id
  - `PUT    /api/history/{id}`   — update record
  - `DELETE /api/history`        — clear all history
  - `DELETE /api/history/{id}`   — delete by id
- DTO layer (`ConversionRequest`, `ConversionResponse`, `HistoryDto`, `ErrorResponse`).
- Global exception handler returning RFC-7807-like JSON error bodies.
- Logback configuration with console + rolling file appenders, 7-day retention,
  and a separate error-only log.
- HTTP request/response logging filter capturing client IP, method, URI, status, latency.
- Springdoc OpenAPI 3 / Swagger UI at `/swagger-ui.html`.
- Spring Boot Actuator health endpoint at `/actuator/health`.
- Systemd unit file and deployment script for Oracle Linux.
- Postman collection with sample requests.
- Unit tests for the parser covering normal, z-continuation, underscore,
  and malformed input scenarios.

### Fixed
- N/A (initial release).

### Release Notes
- Requires **Oracle OpenJDK 17** and **Oracle XE 21c** (or compatible).
- Default port: `8080` (configurable via `SERVER_PORT` env var).
- Database credentials are read from environment variables — do not commit secrets.
