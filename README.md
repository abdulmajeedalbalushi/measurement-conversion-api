# Sequence API — Beginner Spring Boot Project

A tiny REST API that takes a "sequence" string, turns it into a list of numbers, and remembers every sequence it has processed (in memory only — no database).

It uses exactly **4 classes** that follow this UML:

```
SequenceController  ──uses──▶  SequenceService
SequenceService     ──uses──▶  Sequence
SequenceService     ──uses──▶  SequenceHistory
SequenceHistory     ──holds──▶  List<Sequence>
```

---

## Folder structure

```
measurement-conversion-api/
├── pom.xml                                 ← Maven build file
├── README.md                               ← this file
├── postman/
│   └── Sequence.postman_collection.json    ← ready-made Postman requests
└── src/
    └── main/
        ├── java/com/example/sequence/
        │   ├── SequenceApplication.java                ← Spring Boot entry point
        │   ├── controller/SequenceController.java      ← REST endpoints
        │   ├── service/SequenceService.java            ← business logic
        │   └── model/
        │       ├── Sequence.java                       ← one processed sequence
        │       └── SequenceHistory.java                ← in-memory list of sequences
        └── resources/
            └── application.properties                  ← port + app name
```

---

## What each class does

### `SequenceController` — the front door
Receives HTTP requests and returns JSON.
- `GET /api/sequence?input=ab` → process one input
- `GET /api/sequence/history` → return every input processed so far

It does not contain business logic; it just calls the service.

### `SequenceService` — the brain
- Validates the input (only letters and underscores allowed).
- Converts the sequence into a list of numbers using the rule below.
- Builds a `Sequence` object and saves it to `SequenceHistory`.

### `Sequence` — one result
A plain Java object that holds:
- `input` — the original string
- `values` — the list of numbers produced from it

It also validates the format in its constructor (`isValid`).

### `SequenceHistory` — the memory
Wraps an `ArrayList<Sequence>` so the service can:
- `save(sequence)` — add the latest result
- `list()` — return everything seen so far

Because it is annotated `@Component`, Spring creates **one** shared instance for the whole app, so every request sees the same list.

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

You need **Java 17+** and **Maven**.

```bash
mvn spring-boot:run
```

The server starts at `http://localhost:8080`.

---

## API requests and responses

### 1. Process a sequence

```
GET http://localhost:8080/api/sequence?input=dz_a_aazzaaa
```

Response (200 OK):

```json
{
  "input": "dz_a_aazzaaa",
  "values": [28, 53, 1]
}
```

Another example:

```
GET http://localhost:8080/api/sequence?input=za_a_a_a_a_a_a_a_a_a_a_a_a_azaaa
```

```json
{
  "input": "za_a_a_a_a_a_a_a_a_a_a_a_a_azaaa",
  "values": [40, 1]
}
```

Invalid input (digits, spaces, empty string) → 400 Bad Request:

```
GET http://localhost:8080/api/sequence?input=abc123
```

```json
{ "error": "input must be non-empty and contain only letters and underscores" }
```

### 2. Get history

```
GET http://localhost:8080/api/sequence/history
```

Response (200 OK):

```json
[
  { "input": "ab",           "values": [2]         },
  { "input": "dz_a_aazzaaa", "values": [28, 53, 1] }
]
```

History resets every time the app restarts (it lives in RAM only).

---

## Postman testing

Open Postman → **Import** → choose `postman/Sequence.postman_collection.json`.

The collection contains ready-to-send requests:

1. **Process — simple** (`?input=ab`)
2. **Process — z-chain example** (`?input=dz_a_aazzaaa`)
3. **Process — long example** (`?input=za_a_a_..._azaaa`)
4. **Process — invalid input** (returns 400)
5. **History** (`/api/sequence/history`)

Run them in order to see how the history grows.

---

## API flow at a glance

```
Browser / Postman
        │
        │   GET /api/sequence?input=ab
        ▼
SequenceController.process(input)
        │
        ▼
SequenceService.process(input)
        ├── Sequence.isValid(input)            ← format check
        ├── parse(input) → [2]                 ← z-chain decoder
        ├── new Sequence("ab", [2])            ← build result
        └── history.save(sequence)             ← remember it
        │
        ▼
Controller returns JSON  ──▶  {"input":"ab","values":[2]}
```
