# Sequence API — Spring Boot + Oracle XE

A REST API that processes "sequence" strings using the z-chain encoding rule, stores every processed call as a history record in **Oracle XE**, and exposes CRUD endpoints over that history.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Running the Application](#running-the-application)
3. [Configuring the Database](#configuring-the-database)
4. [REST API Endpoints](#rest-api-endpoints)
5. [Deployment](#deployment)
6. [Project Structure](#project-structure)
7. [How the Encoding Works](#how-the-encoding-works)

---

## Prerequisites

| Tool | Minimum version |
|------|----------------|
| Java (JDK) | 17 |
| Maven | 3.8 |
| Oracle XE | 21c |
| Docker (optional) | any recent version |

---

## Running the Application

### 1. Start Oracle XE

**Option A — Docker (recommended)**

```bash
docker run -d --name oracle-xe -p 1521:1521 \
  -e ORACLE_PASSWORD=oracle \
  gvenzl/oracle-xe:21-slim
```

Wait until the container is ready:

```bash
docker logs -f oracle-xe
# Wait for: DATABASE IS READY TO USE!
```

**Option B — Native install**

If you have Oracle XE installed locally, make sure the listener is running on port `1521` and the pluggable database `XEPDB1` is open.

### 2. Clone and build

```bash
git clone <your-repo-url>
cd measurement-conversion-api
mvn clean package -DskipTests
```

### 3. Run

```bash
mvn spring-boot:run
```

Or run the packaged JAR directly:

```bash
java -jar target/sequence-api-0.0.1-SNAPSHOT.jar
```

The server starts at `http://localhost:8080`.

On the **first startup**, Hibernate automatically creates the `HISTORY_RECORD` and `HISTORY_RECORD_OUTPUT` tables in Oracle XE (`spring.jpa.hibernate.ddl-auto=update`).

### Verifying the app is up

```
GET http://localhost:8080/history
```

Expected response: `[]` (empty array if no history yet).

---

## Configuring the Database

Database settings live in `src/main/resources/application.properties`.

```properties
# Oracle XE connection
spring.datasource.url=jdbc:oracle:thin:@localhost:1521/XEPDB1
spring.datasource.username=system
spring.datasource.password=<your-password>
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver

# JPA / Hibernate
spring.jpa.database-platform=org.hibernate.dialect.OracleDialect
spring.jpa.hibernate.ddl-auto=update   # use "validate" or "none" in production
spring.jpa.show-sql=true               # prints SQL to console; turn off in production
spring.jpa.properties.hibernate.format_sql=true
```

### Overriding without editing the file

Use environment variables so you never commit passwords:

```bash
# PowerShell
$env:SPRING_DATASOURCE_URL      = "jdbc:oracle:thin:@localhost:1521/XEPDB1"
$env:SPRING_DATASOURCE_USERNAME = "system"
$env:SPRING_DATASOURCE_PASSWORD = "your_password_here"
mvn spring-boot:run
```

```bash
# Bash / Linux
export SPRING_DATASOURCE_URL=jdbc:oracle:thin:@localhost:1521/XEPDB1
export SPRING_DATASOURCE_USERNAME=system
export SPRING_DATASOURCE_PASSWORD=your_password_here
mvn spring-boot:run
```

### Tables created by Hibernate

```sql
-- Main record table
CREATE TABLE HISTORY_RECORD (
    ID                 NUMBER(19,0) GENERATED AS IDENTITY PRIMARY KEY,
    TIMESTAMP          TIMESTAMP(6) NOT NULL,
    SOURCE_IP_ADDRESS  VARCHAR2(64 CHAR),
    INPUT              VARCHAR2(1024 CHAR) NOT NULL
);

-- Child table for the output list (preserves element order)
CREATE TABLE HISTORY_RECORD_OUTPUT (
    HISTORY_RECORD_ID  NUMBER(19,0) NOT NULL,
    POSITION           NUMBER(10,0) NOT NULL,
    VALUE              NUMBER(10,0),
    PRIMARY KEY (HISTORY_RECORD_ID, POSITION),
    FOREIGN KEY (HISTORY_RECORD_ID) REFERENCES HISTORY_RECORD(ID)
);
```

Verify via `sqlplus`:

```bash
docker exec -it oracle-xe sqlplus system/oracle@//localhost:1521/XEPDB1
SQL> SELECT * FROM HISTORY_RECORD;
SQL> SELECT * FROM HISTORY_RECORD_OUTPUT ORDER BY HISTORY_RECORD_ID, POSITION;
```

---

## REST API Endpoints

Base URL: `http://localhost:8080`

### Endpoint summary

| Method | Path | Description |
|--------|------|-------------|
| GET | `/sequence?input={value}` | Process a sequence; saves a history record |
| GET | `/history` | Return all history records |
| GET | `/history/{id}` | Return one record by id |
| PUT | `/history/{id}` | Update fields of an existing record |
| DELETE | `/history` | Clear all history records |

---

### `GET /sequence?input={value}`

Processes the input string, stores the result, and returns the saved record.

**Request**

```
GET http://localhost:8080/sequence?input=dz_a_aazzaaa
```

**Response — 200 OK**

```json
{
  "id": 1,
  "timestamp": "2026-05-23T10:15:30.123",
  "sourceIpAddress": "127.0.0.1",
  "input": "dz_a_aazzaaa",
  "output": [28, 53, 1]
}
```

**Response — 400 Bad Request** (invalid characters in input)

```json
{ "error": "input must be non-empty and contain only letters and underscores" }
```

---

### `GET /history`

Returns all stored history records.

**Request**

```
GET http://localhost:8080/history
```

**Response — 200 OK**

```json
[
  {
    "id": 1,
    "timestamp": "2026-05-23T10:15:30.123",
    "sourceIpAddress": "127.0.0.1",
    "input": "dz_a_aazzaaa",
    "output": [28, 53, 1]
  }
]
```

---

### `GET /history/{id}`

Returns a single record by its id.

**Request**

```
GET http://localhost:8080/history/1
```

**Response — 200 OK**

```json
{
  "id": 1,
  "timestamp": "2026-05-23T10:15:30.123",
  "sourceIpAddress": "127.0.0.1",
  "input": "dz_a_aazzaaa",
  "output": [28, 53, 1]
}
```

**Response — 404 Not Found**

```json
{ "error": "History record not found: id=99" }
```

---

### `PUT /history/{id}`

Updates one or more fields of an existing record. Only the fields you send are changed. If you update `input` without providing `output`, the output is automatically recomputed.

**Request**

```
PUT http://localhost:8080/history/1
Content-Type: application/json

{
  "input": "ab",
  "sourceIpAddress": "10.0.0.5"
}
```

| Field | Required | Notes |
|-------|----------|-------|
| `input` | No | Must contain only letters and underscores |
| `output` | No | If omitted when `input` changes, output is recomputed |
| `sourceIpAddress` | No | Any string |

**Response — 200 OK**

```json
{
  "id": 1,
  "timestamp": "2026-05-23T10:15:30.123",
  "sourceIpAddress": "10.0.0.5",
  "input": "ab",
  "output": [2]
}
```

**Response — 404 Not Found**

```json
{ "error": "History record not found: id=9999" }
```

---

### `DELETE /history`

Clears every history record from the database.

**Request**

```
DELETE http://localhost:8080/history
```

**Response — 200 OK**

```json
{ "message": "History cleared successfully" }
```

After this, `GET /history` returns `[]`.

---

### Postman collection

Import `postman/Sequence.postman_collection.json` into Postman for ready-to-send requests covering all endpoints.

---

## Deployment

> **Status: Deployment was attempted but did not work.**
>
> The steps below describe the intended deployment approach. The process was not completed successfully — the application could not be deployed to a remote environment. This section is kept here for reference and to document what was tried.

### Intended approach — packaged JAR on a Linux server

**Step 1 — Build the JAR**

```bash
mvn clean package -DskipTests
```

This produces `target/sequence-api-0.0.1-SNAPSHOT.jar`.

**Step 2 — Copy to the server**

```bash
scp target/sequence-api-0.0.1-SNAPSHOT.jar user@your-server:/opt/sequence-api/
```

**Step 3 — Set environment variables on the server**

```bash
export SPRING_DATASOURCE_URL=jdbc:oracle:thin:@<db-host>:1521/XEPDB1
export SPRING_DATASOURCE_USERNAME=system
export SPRING_DATASOURCE_PASSWORD=<password>
```

**Step 4 — Run on the server**

```bash
java -jar /opt/sequence-api/sequence-api-0.0.1-SNAPSHOT.jar
```

**Step 5 — (Optional) Run as a background service**

```bash
nohup java -jar /opt/sequence-api/sequence-api-0.0.1-SNAPSHOT.jar \
  > /var/log/sequence-api.log 2>&1 &
```

### Why deployment did not work

The deployment was not completed. Known blockers:

- **Database connectivity**: The Oracle XE instance was only available on `localhost`. A remote server cannot reach it without additional network configuration (port forwarding, a cloud database, or running Oracle in Docker on the same remote host).
- **No remote server configured**: A target server was not set up and provisioned during development.
- **No CI/CD pipeline**: There is no automated build-and-deploy workflow (GitHub Actions, Jenkins, etc.) in place.

### What would be needed to get deployment working

1. A remote server (e.g. an AWS EC2 instance, Azure VM, or DigitalOcean Droplet) with Java 17+ installed.
2. An Oracle XE instance reachable from that server, **or** switching to a cloud-friendly database (PostgreSQL, MySQL) by swapping the JDBC driver and dialect in `application.properties`.
3. A firewall rule opening port `8080` (or reverse-proxying via Nginx/Apache on port 80/443).
4. Environment variables or a secrets manager providing database credentials to the running process.

---

## Project Structure

```
measurement-conversion-api/
├── pom.xml                                      Maven build file
├── README.md                                    This file
├── version.txt                                  Changelog
├── logs/                                        Rolling log files (created at runtime)
├── postman/
│   └── Sequence.postman_collection.json         Ready-made Postman requests
└── src/main/
    ├── java/com/example/sequence/
    │   ├── SequenceApplication.java             Spring Boot entry point
    │   ├── controller/SequenceController.java   REST endpoints
    │   ├── service/SequenceService.java         Business logic and z-chain parser
    │   ├── repositories/
    │   │   └── HistoryRecordRepository.java     Spring Data JPA repository
    │   └── model/
    │       ├── Sequence.java                    Input format validator
    │       └── HistoryRecord.java               JPA entity (one DB row)
    └── resources/
        ├── application.properties               Port, Oracle XE, JPA config
        └── logback-spring.xml                   Logging configuration
```

---

## How the Encoding Works

A **z-chain** is a sequence of zero or more `z` characters (each worth **26**) followed by exactly one terminator:

| z-chain | Value |
|---------|-------|
| `a`     | 1     |
| `_`     | 0     |
| `zd`    | 30 (26 + 4) |
| `zza`   | 53 (26 + 26 + 1) |

A full input is a series of **packages**:

1. Read one z-chain → that is the **package size** N.
2. Read N more z-chains → those are the **values**.
3. Sum the values → that sum is the package result.
4. Repeat until the input runs out.

**Example: `dz_a_aazzaaa`**

- `d` → size **4**; values: `z_`=26, `a`=1, `_`=0, `a`=1 → sum **28**
- `a` → size **1**; values: `zza`=53 → sum **53**
- `a` → size **1**; values: `a`=1 → sum **1**
- Result: `[28, 53, 1]`

Only letters and underscores are valid input characters. Digits, spaces, or special characters return a 400 error.
