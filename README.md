# Measurement Conversion API

A production-ready Spring Boot REST API that decodes encoded **package
measurement strings** into a list of per-package totals, persists every request
to **Oracle XE**, and is deployable on **Oracle Linux** as a `systemd`
service.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technologies Used](#2-technologies-used)
3. [Architecture](#3-architecture)
4. [Encoding Rules](#4-encoding-rules)
5. [API Reference](#5-api-reference)
6. [Database Configuration](#6-database-configuration)
7. [Oracle XE Setup](#7-oracle-xe-setup)
8. [Running Locally](#8-running-locally)
9. [Building with Maven](#9-building-with-maven)
10. [Deployment on Oracle Linux (via SSH)](#10-deployment-on-oracle-linux-via-ssh)
11. [Logging](#11-logging)
12. [Troubleshooting](#12-troubleshooting)

---

## 1. Project Overview

The API exposes a single conversion endpoint plus a CRUD facade for
request history:

| Method | Path                  | Purpose                             |
| ------ | --------------------- | ----------------------------------- |
| GET    | `/api/conversion`     | Decode an input string              |
| GET    | `/api/history`        | List all conversion records        |
| GET    | `/api/history/{id}`   | Fetch a single record               |
| PUT    | `/api/history/{id}`   | Update a stored record              |
| DELETE | `/api/history/{id}`   | Delete one record                   |
| DELETE | `/api/history`        | Clear all records                   |
| GET    | `/actuator/health`    | Liveness / readiness probe          |
| GET    | `/swagger-ui.html`    | Interactive API documentation       |

---

## 2. Technologies Used

- **Java 17** — Oracle OpenJDK 17
- **Spring Boot 3.2** — Web, Data JPA, Validation, Actuator
- **Maven** — build & dependency management
- **Oracle XE 21c** — relational database
- **HikariCP** — connection pooling
- **Hibernate** — ORM
- **Logback** — production logging with rolling files
- **Springdoc OpenAPI 3** — Swagger UI at runtime
- **JUnit 5 + AssertJ** — unit testing
- **H2** — test profile in-memory DB
- **Lombok** — boilerplate reduction (optional)

---

## 3. Architecture

The code follows a **clean, layered, SRP-driven architecture**. Each layer has
exactly one responsibility, and every cross-layer dependency points
*inward* (controllers depend on services, services depend on repositories;
never the reverse).

```
src/main/java/com/example/measurement/
├── MeasurementConversionApplication.java   <- Boot entry point
├── controller/                             <- HTTP layer (REST)
│     ├── ConversionController.java
│     └── HistoryController.java
├── service/                                <- Application use cases (interfaces)
│     ├── ConversionService.java
│     ├── HistoryService.java
│     └── impl/                             <- Concrete implementations
│           ├── ConversionServiceImpl.java
│           └── HistoryServiceImpl.java
├── repository/                             <- Persistence access
│     └── HistoryRepository.java
├── entity/                                 <- JPA entities (DB shape)
│     └── ConversionHistory.java
├── dto/                                    <- Wire contracts (API shape)
│     ├── ConversionRequest.java
│     ├── ConversionResponse.java
│     ├── HistoryDto.java
│     └── ErrorResponse.java
├── util/                                   <- Stateless helpers / domain logic
│     ├── MeasurementParser.java
│     └── IpAddressUtil.java
├── exception/                              <- Custom exceptions & global handler
│     ├── InvalidInputException.java
│     ├── ResourceNotFoundException.java
│     └── GlobalExceptionHandler.java
└── config/                                 <- Cross-cutting beans
      ├── RequestLoggingFilter.java
      └── OpenApiConfig.java
```

### OOP principles in this codebase

| Principle      | Where you see it                                                                                                  |
| -------------- | ----------------------------------------------------------------------------------------------------------------- |
| **Encapsulation** | Entity / DTO fields are private with explicit accessors; `ConversionResponse.packages` is exposed as unmodifiable. |
| **Abstraction**   | `ConversionService` and `HistoryService` are interfaces; controllers depend only on the abstraction.              |
| **Interfaces**    | Service contracts (`*Service`) and repository contract (`HistoryRepository extends JpaRepository`).               |
| **Inheritance**   | `RequestLoggingFilter extends OncePerRequestFilter`; exceptions extend `RuntimeException`.                        |
| **Polymorphism**  | `@ExceptionHandler` dispatches by runtime exception type; Spring injects whichever `ConversionService` impl exists. |
| **SRP**           | Parser parses, service orchestrates, repository persists, controller handles HTTP. No class does two jobs.        |

### DTO layer (why it's separate from entities)

DTOs (`ConversionRequest`, `ConversionResponse`, `HistoryDto`, `ErrorResponse`)
shape the **wire contract**. Entities (`ConversionHistory`) shape the
**database schema**. Keeping them separate means:

- The DB schema can evolve without breaking API consumers.
- Internal-only fields never leak into JSON.
- Bean Validation (`@NotBlank`, `@Pattern`, …) sits on DTOs, where it belongs —
  the entity uses `@Column(nullable = false)` for DB-level constraints.
- JPA lazy-loading proxies never escape past the service boundary into Jackson.

Mapping between the two happens in the **service layer** (`HistoryServiceImpl#toDto` /
`#toEntity`), never in the controller.

---

## 4. Encoding Rules

The parser alternates between two modes:

### Package mode (header)
- Reads a header that decides how many value characters follow.
- A letter maps alphabetically: `a = 1, b = 2, …, z = 26`.
- **`z` is special — continuation mode.** The parser keeps consuming `z`s,
  adding 26 each time, until it hits the first non-`z` character. That
  terminating letter is *included* in the total.
  - `zd` → `26 + 4 = 30`
  - `zza` → `26 + 26 + 1 = 53`

### Value mode (body)
- Reads exactly `N` characters where `N` is the decoded header.
- Each character → numeric value: `a = 1, …, z = 26, _ = 0`.
- The package's result is the sum of those values.

### Examples

| Input        | Output       | Explanation                                                  |
| ------------ | ------------ | ------------------------------------------------------------ |
| `ab`         | `[2]`        | header `a`=1, body `b`=2                                     |
| `cabc`       | `[6]`        | header `c`=3, body `abc`=1+2+3=6                             |
| `babcdefaz`  | `[3,15,26]`  | three packages: `b\|ab`, `c\|def`, `a\|z`                    |
| `c_b_`       | `[2]`        | header `c`=3, body `_b_`=0+2+0=2                             |

---

## 5. API Reference

### `GET /api/conversion`

Decode a measurement string and persist the result to history.

**Query parameters**

| Name  | Type   | Required | Validation                            |
| ----- | ------ | -------- | ------------------------------------- |
| input | string | yes      | `^[A-Za-z_]+$`, length 1 – 10 000     |

**200 OK**
```http
GET /api/conversion?input=babcdefaz
```
```json
{
  "input": "babcdefaz",
  "packages": [3, 15, 26],
  "packageCount": 3
}
```

**400 Bad Request** — invalid character / malformed structure
```json
{
  "timestamp": "2026-05-21T12:34:56.789Z",
  "status": 400,
  "error": "Invalid input",
  "message": "Package declares size 5 but only 2 character(s) remain",
  "path": "/api/conversion",
  "details": null
}
```

### `GET /api/history`

Returns every stored history record.
```json
[
  {
    "id": 1,
    "timestamp": "2026-05-21T12:34:56.789Z",
    "sourceIpAddress": "127.0.0.1",
    "input": "babcdefaz",
    "output": "[3, 15, 26]"
  }
]
```

### `GET /api/history/{id}` — 200 OK or 404 Not Found.

### `PUT /api/history/{id}`
```http
PUT /api/history/1
Content-Type: application/json

{
  "input": "babcdefaz",
  "output": "[3, 15, 26]",
  "sourceIpAddress": "127.0.0.1"
}
```
Returns the updated record (200 OK) or 404.

### `DELETE /api/history/{id}` → 204 No Content
### `DELETE /api/history`      → 204 No Content (clears all)

### Sample `curl` calls
```bash
curl "http://localhost:8080/api/conversion?input=babcdefaz"
curl "http://localhost:8080/api/history"
curl "http://localhost:8080/api/history/1"
curl -X PUT -H 'Content-Type: application/json' \
     -d '{"input":"ab","output":"[2]"}' \
     "http://localhost:8080/api/history/1"
curl -X DELETE "http://localhost:8080/api/history"
```

A ready-made Postman collection lives in
[`postman/MeasurementConversion.postman_collection.json`](postman/MeasurementConversion.postman_collection.json).

---

## 6. Database Configuration

All credentials are environment-driven; no secret is hard-coded in
`application.properties`.

| Variable     | Default                                       | Purpose                  |
| ------------ | --------------------------------------------- | ------------------------ |
| `SERVER_PORT`| `8080`                                        | HTTP port                |
| `DB_URL`     | `jdbc:oracle:thin:@//localhost:1521/XEPDB1`   | Oracle JDBC URL          |
| `DB_USER`    | `measurement`                                 | DB user                  |
| `DB_PASSWORD`| `change_me_in_env`                            | DB password (set me!)    |
| `LOG_DIR`    | `logs`                                        | Log output directory     |

### Schema (`src/main/resources/schema.sql`)

```sql
CREATE TABLE CONVERSION_HISTORY (
    ID         NUMBER(19)     GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    TS         TIMESTAMP(6)   DEFAULT SYSTIMESTAMP NOT NULL,
    SOURCE_IP  VARCHAR2(64),
    INPUT_VAL  CLOB           NOT NULL,
    OUTPUT_VAL CLOB           NOT NULL
);
```

The file is idempotent — it swallows `ORA-00955` (object exists) so Hibernate's
`ddl-auto=update` can manage the schema thereafter.

---

## 7. Oracle XE Setup

### 7.1 Install Oracle XE 21c on Oracle Linux

```bash
sudo dnf install -y oracle-database-preinstall-21c
sudo dnf install -y \
  https://download.oracle.com/otn-pub/otn_software/db-express/oracle-database-xe-21c-1.0-1.ol8.x86_64.rpm
sudo /etc/init.d/oracle-xe-21c configure
sudo systemctl enable --now oracle-xe-21c
```

### 7.2 Create the application schema

```bash
sqlplus / as sysdba <<'SQL'
ALTER SESSION SET CONTAINER = XEPDB1;
CREATE USER measurement IDENTIFIED BY "ReplaceWithStrongPw#1";
GRANT CONNECT, RESOURCE TO measurement;
ALTER USER measurement QUOTA UNLIMITED ON USERS;
SQL
```

### 7.3 Verify connectivity

```bash
sqlplus measurement/'ReplaceWithStrongPw#1'@//localhost:1521/XEPDB1
```

---

## 8. Running Locally

### Prerequisites
- Oracle OpenJDK 17 — `java -version`
- Maven 3.9+    — `mvn -v`
- Oracle XE running locally (see §7) **or** use the `test` profile (H2).

### Run against Oracle XE

```bash
export DB_URL='jdbc:oracle:thin:@//localhost:1521/XEPDB1'
export DB_USER=measurement
export DB_PASSWORD='ReplaceWithStrongPw#1'

mvn spring-boot:run
```

### Run against in-memory H2 (no Oracle needed)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

Open:
- API:        http://localhost:8080/api/conversion?input=babcdefaz
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health:     http://localhost:8080/actuator/health

---

## 9. Building with Maven

```bash
# Compile + run unit tests
mvn clean test

# Build an executable fat JAR (target/measurement-conversion-api.jar)
mvn clean package

# Skip tests (CI artifact only)
mvn clean package -DskipTests
```

Run the produced JAR directly:
```bash
java -jar target/measurement-conversion-api.jar
```

---

## 10. Deployment on Oracle Linux (via SSH)

### 10.1 Copy the artifact to the host

From your workstation:
```bash
scp target/measurement-conversion-api.jar \
    deployment/measurement-conversion-api.service \
    deployment/app.env.example \
    deployment/deploy.sh \
    opc@<oracle-linux-host>:/home/opc/
```

### 10.2 Run the deploy script

```bash
ssh opc@<oracle-linux-host>
sudo dnf install -y java-17-openjdk          # if not already installed
cd /home/opc
sudo bash deploy.sh measurement-conversion-api.jar
```

The script:
1. Creates a `measurement` system user (no shell, no home).
2. Lays down `/opt/measurement-conversion-api/` and `/var/log/measurement-conversion-api/`.
3. Installs the systemd unit at `/etc/systemd/system/measurement-conversion-api.service`.
4. Seeds `/etc/measurement-conversion-api/app.env` from the example — **edit it**
   to set real DB credentials, then restart the service.
5. Opens port 8080/tcp in `firewalld`.
6. Enables + starts the service.

### 10.3 Set real credentials and restart

```bash
sudo vi /etc/measurement-conversion-api/app.env       # set DB_PASSWORD etc.
sudo systemctl restart measurement-conversion-api
sudo systemctl status  measurement-conversion-api
```

### 10.4 Run-in-background commands (without systemd)

```bash
# Background, detached, logging to a file
nohup java -jar /opt/measurement-conversion-api/measurement-conversion-api.jar \
    > /var/log/measurement-conversion-api/console.out 2>&1 &
disown

# Check
ps -ef | grep measurement-conversion-api
```

### 10.5 Firewall / port checklist

```bash
# Open 8080
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload

# In Oracle Cloud, also add an ingress rule on the VCN security list
# for TCP/8080 from your source CIDR.
```

### 10.6 Smoke test from your workstation
```bash
curl "http://<oracle-linux-host>:8080/api/conversion?input=babcdefaz"
```

---

## 11. Logging

Logback is configured in `src/main/resources/logback-spring.xml`.

| Appender    | Destination                                                | Rotation              |
| ----------- | ---------------------------------------------------------- | --------------------- |
| Console     | stdout                                                     | n/a                   |
| File        | `${LOG_DIR}/measurement-conversion-api.log`                | daily + 50 MB, 7 days |
| Error file  | `${LOG_DIR}/measurement-conversion-api-error.log` (WARN+)  | daily + 20 MB, 7 days |

Every HTTP request gets a UUID request id (stored in the SLF4J MDC) which is
printed in the `[…]` slot of every log line — making it trivial to grep a
full request lifecycle.

Tail logs in production:
```bash
journalctl -u measurement-conversion-api -f
tail -f /var/log/measurement-conversion-api/measurement-conversion-api.log
tail -f /var/log/measurement-conversion-api/measurement-conversion-api-error.log
```

---

## 12. Troubleshooting

| Symptom                                                                | Likely cause / fix                                                                                                            |
| ---------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| `ORA-12541: TNS:no listener`                                           | Oracle XE not running. `sudo systemctl status oracle-xe-21c`.                                                                  |
| `ORA-01017: invalid username/password`                                 | `DB_USER` / `DB_PASSWORD` mismatch — verify with `sqlplus`.                                                                    |
| `ORA-12514: TNS:listener does not currently know of service requested` | Use the PDB name (`XEPDB1`) in the JDBC URL, not the CDB.                                                                      |
| App starts, port 8080 not reachable                                    | `firewalld` rule missing **and/or** OCI VCN ingress missing.                                                                   |
| `400 Bad Request` on every request                                     | Input contains digits/punctuation — only `[A-Za-z_]` is accepted.                                                              |
| Log files not appearing                                                | `LOG_DIR` directory not writable by the `measurement` user. `chown -R measurement:measurement /var/log/measurement-conversion-api`. |
| Service fails to start under systemd                                   | `journalctl -u measurement-conversion-api -e` — usually a typo in `/etc/measurement-conversion-api/app.env`.                   |
| `OutOfMemoryError`                                                     | Raise heap in the unit file's `ExecStart` (e.g. `-Xmx1g`).                                                                     |

---

## License

MIT. See `CHANGELOG.md` for release notes.
#   m e a s u r e m e n t - c o n v e r s i o n - a p i  
 