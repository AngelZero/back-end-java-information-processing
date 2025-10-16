
# Back-End in Java for Information Processing — Sprint 3

This repository now contains the **full JSON → CSV desktop back-end** required by the Digital NAO challenge:

- **Reader**: loads JSON into a `JsonNode` tree (Jackson)
- **Normalizer**: maps arbitrary JSON (objects/arrays/nesting) into **one or more CSV “tables”**
- **Writer**: writes CSVs with configurable **dialect** and **cell formatting** (Apache Commons CSV)
- **CLI**: all behaviors are configurable via command-line flags
- **Tests**: JUnit 5 unit + end-to-end tests covering the main combinations
- **JavaDoc**: for public classes/methods

> Sprint 2 deliverables (JSON reader, CSV writer, tests, JavaDoc) are preserved.  
> Sprint 3 adds **normalization, orchestration, CLI**, and **E2E coverage**.

---

## How it works

1. **JsonReader** (`ReaderAndConverter.Reader.JsonReader`)  
   Parses a file into a Jackson `JsonNode` with robust error handling.

2. **JsonNormalizer** (`ReaderAndConverter.Converter.JsonNormalizer`)  
   Walks the JSON tree and produces one or more tables based on `NormalizerConfig`:
   - **Nested objects** → same table or child table (configurable).
   - **Arrays of objects** → child table or inline as JSON (configurable via `arrayStrategy`).
   - **Arrays of primitives** → join into one cell or explode to a child table.
   - Optional **`id`** and **`parent_id`** columns to link rows.
   - Column **header order**: `sorted` or `encountered`.

3. **CsvWriter** (`ReaderAndConverter.Converter.CsvWriter`)  
   Writes rows in a fixed header order. `CsvFormatOptions` controls delimiter, quoting, record separator, header on/off, null literal, encoding, Excel-safe, inline-objects-as-JSON, datetime format, and joiners.

4. **Application** (`ReaderAndConverter.App.Application`)  
   Orchestrates: **read → normalize → write**.

5. **Main** (`ReaderAndConverter.App.Main`)  
   Minimal CLI that builds `AppConfig` from flags and delegates to `Application`.

---

## CLI Usage

### Most useful flags

**Basic**

* `--in <path>` input JSON (required)
* `--out <dir>` output directory (default: `out`)
* `--help`

**CSV dialect (CsvFormatOptions)**

* `--delimiter <char>` (`,`, `;`, `|`, `\t`, …)
* `--quote <char>`
* `--record-sep <str>` (`\n`, `\r\n`, or any string)
* `--quote-mode <minimal|all|none|non_numeric>`
* `--print-header` | `--no-header`
* `--null-literal <str>`
* `--encoding <name>` (default `UTF-8`)
* `--csv-array-join <str>` (join for collections inside one cell)
* `--excel-safe <true|false>` (prefix `'` to `= + - @`)
* `--object-inline-json <true|false>` (Map → JSON text)
* `--datefmt <pattern|ISO_INSTANT>`

**Normalizer (NormalizerConfig)**

* `--root-table <name>` (default: `root`)
* `--allow-nested <true|false>` or `--no-nested`
* `--array-strategy <explode|inline>` (arrays of objects)
* `--primitive-array <join|explode>` (arrays of primitives)
* `--add-id <true|false>` (default: true)
* `--add-parent-id <true|false>` (default: true)
* `--no-ids` (shorthand for both above = false)
* `--id-field <name>` / `--parent-id-field <name>`
* `--norm-join <str>` (join used by normalizer for primitive arrays)
* `--max-depth <int>` (safety guard; default 64)
* `--header-order <sorted|encountered>`

---



## Tests

* **Unit tests** for reader/writer.
* **End-to-end tests**:

  * `ApplicationCliOptionsTest` (core scenarios)
  * `AdditionalNormalizationOptionsTest` (array strategies, primitive arrays explode, header order, null literal, Excel-safe, etc.)

Run all tests:

```bash
mvn -q test
```

---

## JavaDoc

Generate HTML docs to `target/site/apidocs`:

```bash
mvn -q javadoc:javadoc
```

---

## Repository structure

```
src/
  main/java/ReaderAndConverter/
    App/        # Main, Application, AppConfig
    Reader/     # JsonReader 
    Converter/  # JsonNormalizer, NormalizerConfig, CsvWriter, CsvFormatOptions, ValueFormatter, DefaultValueFormatter
    Model/      # Table, TableRegistry
    Exceptions/ #CsvWriteException, JsonReadException
  test/java/ReaderAndConverter/
    App/        # E2E tests
    ...         # Unit tests
test resources/
  samples/  # test JSONs used by E2E tests
samples/                        # manual JSONs for quick runs
out/                            # generated CSVs (ignored in VCS)
```

---

## Tools used

* Java 17+ (tested with 18)
* Maven 3.9+
* Jackson (databind)
* Apache Commons CSV
* JUnit 5


