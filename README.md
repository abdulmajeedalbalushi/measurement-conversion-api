# Sequence API — Beginner Spring Boot Project

A tiny REST API that takes a "sequence" string, turns it into a list of numbers, and remembers every sequence it has processed — now persisted in **Oracle XE** through **Spring Data JPA + Hibernate**.

Each processed call is stored as a **history record** with an id, timestamp, source IP, the original input, and the output list. History records can be read individually, updated via PUT, and cleared via DELETE — all backed by the database.

It uses these classes:

```
SequenceController       ──uses──▶  SequenceService
SequenceService          ──uses──▶  Sequence (validation helper)
SequenceService          ──uses──▶  HistoryRecordRepository
HistoryRecordRepository  ──backed-by──▶  Oracle XE (table history_record)
SequenceService          ──creates──▶  HistoryRecord (JPA @Entity)
```

- **Controller** is the front door (HTTP in, JSON out).
- **Service** does the business logic (validate, parse, build records, ask the repository to store/update).
- **Repository** is a Spring Data JPA interface — Spring writes the SQL for us.
- **Models** (`HistoryRecord`, `Sequence`) hold data / validate format. `HistoryRecord` is a JPA entity that maps to a database table.

---

## Folder structure

```
measurement-conversion-api/
├── pom.xml                                 ← Maven build file
├── README.md                               ← this file
├── version.txt                             ← changelog (version history)
├── logs/                                   ← created at runtime (rolling log files)
├── postman/
│   └── Sequence.postman_collection.json    ← ready-made Postman requests
└── src/
    └── main/
        ├── java/com/example/sequence/
        │   ├── SequenceApplication.java                ← Spring Boot entry point
        │   ├── controller/SequenceController.java      ← REST endpoints (+ logs)
        │   ├── service/SequenceService.java            ← business logic (+ logs)
        │   ├── repositories/
        │   │   └── HistoryRecordRepository.java        ← Spring Data JPA repository
        │   └── model/
        │       ├── Sequence.java                       ← input format validator
        │       └── HistoryRecord.java                  ← JPA @Entity (one DB row)
        └── resources/
            ├── application.properties                  ← port, Oracle XE, JPA config
            └── logback-spring.xml                      ← logging configuration
```

---

## What each class does

### `SequenceController` — the front door
Receives HTTP requests and returns JSON. It does not contain business logic; it just calls the service.

| Method | URL                | Purpose                                |
|--------|--------------------|----------------------------------------|
| GET    | `/sequence?input=` | Process a sequence; save a record      |
| GET    | `/history`         | Return all history records             |
| GET    | `/history/{id}`    | Return one history record by id        |
| PUT    | `/history/{id}`    | Update an existing record's fields     |
| DELETE | `/history`         | Clear every stored history record      |

### `SequenceService` — the brain
- Validates the input (only letters and underscores allowed).
- Converts the sequence into a list of numbers using the z-chain rule (see below).
- Builds a `HistoryRecord` with a fresh `timestamp` and the caller's IP.
- Asks `HistoryRecordRepository` to save / fetch / update / delete records.
- Marked with `@Transactional` on write methods so Hibernate flushes the changes in one database transaction.

### `Sequence` — the format validator
A plain helper class. The static method `Sequence.isValid(input)` checks that the string is non-empty and contains only letters and underscores. The service uses it to validate both new and updated inputs.

### `HistoryRecord` — one row of history (JPA @Entity)
A JPA entity mapped to the `history_record` table:
- `Long id` — primary key, auto-assigned by Oracle (`@GeneratedValue(IDENTITY)`)
- `LocalDateTime timestamp` — when the record was created
- `String sourceIpAddress` — IP of the HTTP caller
- `String input` — the original input string
- `List<Integer> output` — the numeric result, stored in a child table `history_record_output` via `@ElementCollection` (preserves order with `@OrderColumn`).

### `HistoryRecordRepository` — the database access layer
A Spring Data JPA interface:

```java
@Repository
public interface HistoryRecordRepository extends JpaRepository<HistoryRecord, Long> {
}
```

We don't write the implementation — Spring Data writes it for us at runtime. Extending `JpaRepository` gives us `save`, `findAll`, `findById`, `deleteAll`, `count`, `existsById`, and many more for free.

---

## How the encoding works (z-chain)

A "z-chain" is the building block:
- Zero or more `z` characters (each worth **26**) followed by
- exactly **one** terminator character (`a`=1, `b`=2, …, `z`=26, `_`=0).

Examples of z-chains:

| z-chain | value |
|---------|-------|
| `a`     | 1     |
| `_`     | 0     |
| `zd`    | 26 + 4 = 30 |
| `zza`   | 26 + 26 + 1 = 53 |
| `z_`    | 26 + 0 = 26 |

A full input is a series of **packages**. For each package:
1. Read one z-chain — that's the **package size** N.
2. Read N more z-chains — those are the **values**.
3. Sum the values; that sum is the package result.
4. Repeat until the input runs out.

### Worked example: `dz_a_aazzaaa`
- Header `d` → size **4**
- Four values: `z_` = 26, `a` = 1, `_` = 0, `a` = 1 → sum **28**
- Header `a` → size **1**
- One value: `zza` = 53 → sum **53**
- Header `a` → size **1**
- One value: `a` = 1 → sum **1**
- Final answer: **`[28, 53, 1]`**

---

## Running the app

You need **Java 17+**, **Maven**, and a running **Oracle XE 21c** instance.

### Start Oracle XE (Docker — simplest)

```bash
docker run -d --name oracle-xe -p 1521:1521 \
  -e ORACLE_PASSWORD=oracle gvenzl/oracle-xe:21-slim
```

Wait until `docker logs -f oracle-xe` prints **"DATABASE IS READY TO USE!"**. The default service name is `XEPDB1`, user `system`, password `oracle` — those values match the defaults in `application.properties`. If you installed Oracle XE natively, override the URL / username / password in `application.properties` to match.

### Start the API

```bash
mvn spring-boot:run
```

The server starts at `http://localhost:8080`. On first startup Hibernate creates the `history_record` and `history_record_output` tables automatically (`spring.jpa.hibernate.ddl-auto=update`).

---

## API requests and responses

### 1. Process a sequence — `GET /sequence`

```
GET http://localhost:8080/sequence?input=dz_a_aazzaaa
```

Response (200 OK):

```json
{
  "id": 1,
  "timestamp": "2026-05-23T10:15:30.123",
  "sourceIpAddress": "127.0.0.1",
  "input": "dz_a_aazzaaa",
  "output": [28, 53, 1]
}
```

Another example:

```
GET http://localhost:8080/sequence?input=za_a_a_a_a_a_a_a_a_a_a_a_a_azaaa
```

```json
{
  "id": 2,
  "timestamp": "2026-05-23T10:16:02.987",
  "sourceIpAddress": "127.0.0.1",
  "input": "za_a_a_a_a_a_a_a_a_a_a_a_a_azaaa",
  "output": [40, 1]
}
```

Invalid input (digits, spaces, empty string) → 400 Bad Request:

```
GET http://localhost:8080/sequence?input=abc123
```

```json
{ "error": "input must be non-empty and contain only letters and underscores" }
```

### 2. Get all history — `GET /history`

```
GET http://localhost:8080/history
```

Response (200 OK):

```json
[
  {
    "id": 1,
    "timestamp": "2026-05-23T10:15:30.123",
    "sourceIpAddress": "127.0.0.1",
    "input": "dz_a_aazzaaa",
    "output": [28, 53, 1]
  },
  {
    "id": 2,
    "timestamp": "2026-05-23T10:16:02.987",
    "sourceIpAddress": "127.0.0.1",
    "input": "za_a_a_a_a_a_a_a_a_a_a_a_a_azaaa",
    "output": [40, 1]
  }
]
```

History resets every time the app restarts (it lives in RAM only).

### 3. Get one record — `GET /history/{id}`

```
GET http://localhost:8080/history/1
```

```json
{
  "id": 1,
  "timestamp": "2026-05-23T10:15:30.123",
  "sourceIpAddress": "127.0.0.1",
  "input": "dz_a_aazzaaa",
  "output": [28, 53, 1]
}
```

If the id does not exist → 404 Not Found:

```json
{ "error": "History record not found: id=99" }
```

### 4. Update a record — `PUT /history/{id}`

You can update **any of three fields**: `input`, `output`, `sourceIpAddress`. Only the fields you send are changed; the others stay the same. The `id` and `timestamp` are never changed.

If you send a new `input` but no `output`, the service automatically re-parses the input so the output stays consistent.

Request:

```
PUT http://localhost:8080/history/1
Content-Type: application/json

{
  "input": "ab",
  "sourceIpAddress": "10.0.0.5"
}
```

Response (200 OK) — note `output` was recomputed from the new input:

```json
{
  "id": 1,
  "timestamp": "2026-05-23T10:15:30.123",
  "sourceIpAddress": "10.0.0.5",
  "input": "ab",
  "output": [2]
}
```

Update only the IP:

```
PUT http://localhost:8080/history/1
Content-Type: application/json

{ "sourceIpAddress": "192.168.1.42" }
```

Update fails with 404 when the id does not exist:

```json
{ "error": "History record not found: id=9999" }
```

### 5. Clear all history — `DELETE /history`

```
DELETE http://localhost:8080/history
```

Response (200 OK):

```json
{ "message": "History cleared successfully" }
```

After this call, `GET /history` returns `[]`, and the next saved record starts again at `id = 1`.

---

## What HTTP DELETE means (for beginners)

HTTP has a small set of verbs that describe what you want to do with a resource. The most common ones are:

| Verb     | Meaning                                       |
|----------|-----------------------------------------------|
| `GET`    | "Give me this thing." (read)                  |
| `POST`   | "Create a new thing."                         |
| `PUT`    | "Replace / update this existing thing."       |
| `DELETE` | "Remove this thing."                          |

`DELETE` tells the server: *please remove the resource at this URL*. The client does not send a body — the URL itself is the instruction.

- `DELETE /history/1` would mean "remove the history record with id 1."
- `DELETE /history` means "remove **all** history records."

In this project, `DELETE /history` empties the in-memory list and resets the id counter. After that:

- `GET /history` returns `[]`
- The next `GET /sequence?input=...` saves a record with `id = 1` again

That's it — no body, no parameters, just the URL and the verb.

---

## How PUT updates a record (for beginners)

PUT is one of the four most common HTTP methods. Think of it as the verb **"replace this thing"**.

When a client sends:

```
PUT /history/1
{ "input": "ab", "sourceIpAddress": "10.0.0.5" }
```

…here is what happens, step by step:

1. **Spring routes the request.** The URL pattern `/history/{id}` matches, and Spring puts the `1` into the `id` method parameter.
2. **Spring reads the JSON body.** Jackson (a JSON library that comes with Spring Boot) turns the body into an `UpdateHistoryRequest` object with three fields: `input`, `output`, and `sourceIpAddress`. Any field the client did not send is left as `null`.
3. **The controller calls the service.** It hands the id and the three fields to `SequenceService.updateHistory(...)`.
4. **The service validates the new input.** If the client sent a new `input`, it checks the format. If it sent no `output`, the service re-runs the z-chain parser to keep the output in sync.
5. **The history store finds and edits the record.** `SequenceHistory.updateById(...)` walks the list, finds the record whose `id` matches, and overwrites only the fields the caller supplied. `id` and `timestamp` are never touched.
6. **The controller returns the updated record as JSON.** If no record had that id, the controller returns HTTP 404 instead.

The important idea: PUT does **not** create a new record. It changes the one that already exists. The request URL tells you which record (`/history/1`), and the JSON body tells you what to change.

---

## Database persistence (Oracle XE + JPA)

Records used to live in a `synchronized ArrayList` and disappeared on restart. Now they're persisted to Oracle XE through Spring Data JPA, so they survive restarts, crashes, and deploys.

### What JPA is

**JPA** (Jakarta Persistence API) is a Java standard that says: *"if you mark a class with `@Entity`, the framework will map it to a database table for you."* It's just a specification — a set of annotations and interfaces — not an engine. You annotate plain Java classes; the engine does the SQL.

### What Hibernate does

**Hibernate** is the most popular JPA implementation, and the one Spring Boot pulls in by default when you add `spring-boot-starter-data-jpa`. It's the engine that:
- Reads your `@Entity` classes at startup and figures out the tables and columns.
- Generates the SQL (`INSERT`, `SELECT`, `UPDATE`, `DELETE`) at runtime.
- Talks to the database through the JDBC driver.
- Manages a unit of work called a *persistence context* — basically, an in-memory cache of entities inside a transaction.

If you change databases (Oracle → Postgres → MySQL), Hibernate changes the generated SQL accordingly. Your Java code stays the same.

### What a Repository is

A **repository** is a Spring Data interface that you *declare* but never implement:

```java
@Repository
public interface HistoryRecordRepository extends JpaRepository<HistoryRecord, Long> {
}
```

That single line gives you `save`, `findAll`, `findById`, `deleteAll`, `count`, `existsById`, `findAllById`, and dozens more. Spring writes the implementation at runtime by reading the method names. You can also add custom queries by declaring methods like `List<HistoryRecord> findBySourceIpAddress(String ip)` — Spring derives the SQL from the method name.

### Memory storage vs. database persistence

| | In-memory (old) | Database (new) |
|---|---|---|
| Survives restart? | No — every restart wipes the list. | Yes — rows live in Oracle. |
| Scales beyond one process? | No — each JVM has its own list. | Yes — many app instances share one DB. |
| Crash safe? | No. | Yes — committed transactions are durable. |
| Query power | Just `for` loops over `List`. | Full SQL: indexes, joins, ordering, pagination. |
| Latency | Microseconds (RAM). | Milliseconds (network + disk). |
| Setup cost | Zero. | Needs a running database server. |

In real projects you almost always want a database. The in-memory version was useful for the first few lessons; this lesson upgrades to the production-shaped pattern.

### How Spring Boot connects to Oracle XE

Four pieces line up:

1. **JDBC driver on the classpath** — `pom.xml` includes `com.oracle.database.jdbc:ojdbc11`. The driver is the low-level library that knows how to speak Oracle's wire protocol.
2. **Datasource URL + credentials** in `application.properties`:
   ```properties
   spring.datasource.url=jdbc:oracle:thin:@localhost:1521/XEPDB1
   spring.datasource.username=system
   spring.datasource.password=oracle
   spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
   ```
3. **JPA / Hibernate config** — also in `application.properties`:
   ```properties
   spring.jpa.database-platform=org.hibernate.dialect.OracleDialect
   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.show-sql=true
   ```
   `OracleDialect` tells Hibernate which SQL flavour to emit. `ddl-auto=update` lets Hibernate create/extend tables to match your `@Entity` classes on startup. `show-sql=true` prints every SQL statement to the console while you're learning.
4. **Your code** — `@Entity HistoryRecord`, `HistoryRecordRepository`, and `@Autowired` injection in the service. Spring Boot wires it all up automatically at startup.

### Resulting SQL schema

Hibernate creates these tables on first run:

```sql
CREATE TABLE HISTORY_RECORD (
    ID                 NUMBER(19,0) GENERATED AS IDENTITY PRIMARY KEY,
    TIMESTAMP          TIMESTAMP(6) NOT NULL,
    SOURCE_IP_ADDRESS  VARCHAR2(64 CHAR),
    INPUT              VARCHAR2(1024 CHAR) NOT NULL
);

CREATE TABLE HISTORY_RECORD_OUTPUT (
    HISTORY_RECORD_ID  NUMBER(19,0) NOT NULL,
    POSITION           NUMBER(10,0) NOT NULL,
    VALUE              NUMBER(10,0),
    PRIMARY KEY (HISTORY_RECORD_ID, POSITION),
    FOREIGN KEY (HISTORY_RECORD_ID) REFERENCES HISTORY_RECORD(ID)
);
```

### Example data stored in Oracle XE

After calling `GET /sequence?input=dz_a_aazzaaa`:

`HISTORY_RECORD`:

| ID | TIMESTAMP                | SOURCE_IP_ADDRESS | INPUT          |
|----|--------------------------|-------------------|----------------|
| 1  | 2026-05-23 10:15:30.123  | 127.0.0.1         | dz_a_aazzaaa   |

`HISTORY_RECORD_OUTPUT`:

| HISTORY_RECORD_ID | POSITION | VALUE |
|-------------------|----------|-------|
| 1                 | 0        | 28    |
| 1                 | 1        | 53    |
| 1                 | 2        | 1     |

You can confirm directly from `sqlplus`:

```bash
docker exec -it oracle-xe sqlplus system/oracle@//localhost:1521/XEPDB1
SQL> SELECT * FROM HISTORY_RECORD;
SQL> SELECT * FROM HISTORY_RECORD_OUTPUT ORDER BY HISTORY_RECORD_ID, POSITION;
```

### Why this matters

Run `GET /sequence?input=ab`. Stop the app. Start it again. Hit `GET /history` — your record is **still there**. That's the whole point. In the old version it would have been gone.

---

## Logging

The app prints log lines to the console **and** writes them to files under `logs/`. Files rotate once per day, and anything older than 7 days is deleted automatically.

Sample console output for a single `GET /sequence?input=ab` call:

```
2026-05-24 10:15:30.123 INFO  [http-nio-8080-exec-1] c.e.s.controller.SequenceController - API request received: GET /sequence input='ab' from 127.0.0.1
2026-05-24 10:15:30.130 INFO  [http-nio-8080-exec-1] c.e.s.service.SequenceService       - Saved history record id=1 input='ab' output=[2]
2026-05-24 10:15:30.131 INFO  [http-nio-8080-exec-1] c.e.s.controller.SequenceController - Sequence processed successfully: id=1 output=[2]
```

Sample for an invalid input:

```
2026-05-24 10:16:02.987 INFO  [http-nio-8080-exec-2] c.e.s.controller.SequenceController - API request received: GET /sequence input='abc123' from 127.0.0.1
2026-05-24 10:16:02.989 WARN  [http-nio-8080-exec-2] c.e.s.service.SequenceService       - Rejecting invalid input 'abc123'
2026-05-24 10:16:02.990 WARN  [http-nio-8080-exec-2] c.e.s.controller.SequenceController - Invalid input detected on GET /sequence: 'abc123' — input must be non-empty and contain only letters and underscores
```

Sample for a DELETE:

```
2026-05-24 10:17:11.555 INFO  [http-nio-8080-exec-3] c.e.s.controller.SequenceController - API request received: DELETE /history
2026-05-24 10:17:11.556 INFO  [http-nio-8080-exec-3] c.e.s.service.SequenceService       - Clearing all history records
2026-05-24 10:17:11.557 INFO  [http-nio-8080-exec-3] c.e.s.model.SequenceHistory         - History cleared (3 record(s) removed)
2026-05-24 10:17:11.558 INFO  [http-nio-8080-exec-3] c.e.s.controller.SequenceController - History cleared
```

### What logging is (for beginners)

A **log** is a single line of text that the app prints to record something that happened — a request arrived, a record was saved, an input was rejected, an error occurred. The library that does this is called **SLF4J** (the API) backed by **Logback** (the engine), which Spring Boot ships with by default.

In Java code, each class gets its own logger:

```java
private static final Logger log = LoggerFactory.getLogger(SequenceService.class);

log.info("Saved history record id={} input='{}'", id, input);
log.warn("Rejecting invalid input '{}'", input);
log.error("Something went very wrong", exception);
```

Each call has a **level** that says how serious the message is:

| Level | Meaning                                            |
|-------|----------------------------------------------------|
| TRACE | Very fine-grained, usually off                     |
| DEBUG | Internal details useful while developing           |
| INFO  | Normal events you'd want to see in production      |
| WARN  | Something unexpected happened, but the app coped   |
| ERROR | Something went wrong                               |

This project logs at **INFO** by default (set in `logback-spring.xml`).

### Why logs are important

- **Debugging:** when a bug happens in production, you can't attach a debugger. The logs are your only view into what the app actually did.
- **Monitoring:** logs tell you the app is alive, which requests it served, and how it behaved.
- **Auditing:** logs leave a trail of who did what and when (e.g. which IP called `DELETE /history`).
- **Performance:** timing information in logs helps spot slow operations.

### Console logs vs. file logs

- **Console logs** appear in the terminal where you ran the app. They're great while you're developing — you see things immediately. But when you close the terminal, they're gone.
- **File logs** are written to disk under `logs/`. They survive restarts and can be opened later, copied, searched with `grep`, or shipped to a log aggregator (like ELK or Datadog) in a real production setup.

Most real apps do both: console for live feedback, files for history.

### What "rolling logs" mean

If we wrote every log line to a single file forever, it would eventually fill the disk. **Rolling** means the file is rotated automatically — usually once a day. Today's logs go into `sequence-api.log`; at midnight, that file is renamed `sequence-api.2026-05-24.log` and a fresh `sequence-api.log` is started. Files older than 7 days are deleted automatically.

Configuration lives in `src/main/resources/logback-spring.xml`:

- `fileNamePattern` — the daily filename pattern (`sequence-api.%d{yyyy-MM-dd}.log`).
- `maxHistory` — how many days of log files to keep (we use `7`).

You'll see files like this after running the app for a few days:

```
logs/
├── sequence-api.log              ← today, currently being written
├── sequence-api.2026-05-23.log   ← yesterday
├── sequence-api.2026-05-22.log
└── ...                           ← up to 7 days back
```

---

## Changelog (version.txt)

`version.txt` lives at the project root and records the features added in each version of the API:

```
v1.0 - Initial sequence API
v1.1 - Added history records (id, timestamp, sourceIpAddress, input, output)
v1.2 - Added PUT update endpoint for history records
v1.3 - Added DELETE history endpoint to clear all records
v1.4 - Added logging system (console + rolling daily files, 7-day retention)
```

### Why changelogs are useful in real projects

- **Teammates can catch up at a glance** — "what changed since I last looked at this?" is answered in one file.
- **Bug reports get clearer** — "happens in v1.3, didn't happen in v1.2" narrows the search.
- **Deployments are safer** — release notes for ops/QA come straight from the changelog.
- **Users of the API can plan upgrades** — they know which version brought a feature they want.

Update `version.txt` every time you add a meaningful feature or fix.

---

## Postman testing

Open Postman → **Import** → choose `postman/Sequence.postman_collection.json`.

The collection contains ready-to-send requests:

1. **Process — simple** (`/sequence?input=ab`)
2. **Process — z-chain example** (`/sequence?input=dz_a_aazzaaa`)
3. **Process — long example** (`/sequence?input=za_a_a_..._azaaa`)
4. **Process — invalid input** (returns 400)
5. **History — get all** (`GET /history`)
6. **History — get by id** (`GET /history/1`)
7. **History — update by id (PUT)** — full update of input, output, and IP
8. **History — update only IP (PUT)** — demonstrates partial update
9. **History — clear all (DELETE)** — empties the Oracle XE tables
10. **History — update unknown id** — returns 404

Run them in order to see how the history grows, how PUT changes a record in place, and how DELETE wipes the tables. Restart the app between requests and confirm the data is still there — that's the whole point of database persistence.

---

## API flow at a glance

```
Browser / Postman
        │
        │   GET /sequence?input=ab
        ▼
SequenceController.processSequence(input, request)
        │   reads client IP from the request
        ▼
SequenceService.process(input, ip)
        ├── Sequence.isValid(input)              ← format check
        ├── parse(input) → [2]                   ← z-chain decoder
        ├── new HistoryRecord(timestamp, ip, …)  ← build entity
        └── repo.save(record)                    ← Hibernate INSERTs into Oracle
        │
        ▼
Controller returns JSON  ──▶  { "id": 1, "timestamp": "...", ... }


Browser / Postman
        │
        │   PUT /history/1   { "input": "ab" }
        ▼
SequenceController.updateHistory(1, body)
        │
        ▼
SequenceService.updateHistory(1, "ab", null, null)
        ├── repo.findById(1)                     ← fetch row from Oracle
        ├── Sequence.isValid("ab")               ← validate new input
        ├── parse("ab") → [2]                    ← recompute output
        ├── mutate fields (id/timestamp untouched)
        └── repo.save(updated)                   ← Hibernate UPDATEs the row
        │
        ▼
Controller returns the updated record as JSON
```
