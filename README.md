# Back-End in Java for Information Processing (Sprint 2)

This repository contains the **Sprint 2 deliverables** for the Digital NAO challenge:
- A **JSON reader** (Jackson) that loads files into a `JsonNode` tree
- A **CSV writer** (Apache Commons CSV) that outputs rows with configurable **dialect** (delimiter, quoting, record separator, header, null literal) and **cell formatting** (dates, numbers, booleans, arrays, inline JSON, Excel-safe)
- **Unit tests** (JUnit 5)
- **JavaDoc** for all classes and public methods

> Sprint 3 (to be added later) will implement the **mapping/normalizer** from generic JSON to one or multiple CSV tables, driven by a configuration file.

---

## Repository structure

- src/
- main/
- java/
- ReaderAndConverter/
- Reader/ # JsonReader (+ custom JsonReadException)
- Converter/ # CsvWriter, CsvFormatOptions, ValueFormatter, DefaultValueFormatter (+ CsvWriteException)
- Exceptions/ # Exceptions used by Reader/Writer
- resources/ # (reserved for configs/samples later)
- test/
- java/ # JUnit tests for Reader and Writer
- resources/ # (optional sample files)

## Requirements

- **Java 17+**
- **Maven 3.9+**

---
