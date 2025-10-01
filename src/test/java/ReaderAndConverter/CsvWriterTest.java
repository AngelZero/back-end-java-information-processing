package ReaderAndConverter;

import ReaderAndConverter.Converter.CsvFormatOptions;
import ReaderAndConverter.Converter.CsvWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CsvWriterTest {

    @TempDir
    Path temp;

    // Utility to read file content
    private String read(Path p) throws Exception {
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    @Test
    void writesCsv_defaultDialect_andFormats() throws Exception {
        var headers = List.of("id", "name", "price", "tags", "active", "meta");
        var row1 = new HashMap<String, Object>();
        row1.put("id", 7);
        row1.put("name", "Paper A");
        row1.put("price", new BigDecimal("12345.67"));
        row1.put("tags", List.of("ml", "csv"));
        row1.put("active", true);
        row1.put("meta", Map.of("aid", 7, "owner", "Beatriz"));

        var row2 = new HashMap<String, Object>();
        row2.put("id", 8);
        row2.put("name", "Paper B");
        row2.put("price", null);
        row2.put("tags", List.of()); // empty list
        row2.put("active", false);
        row2.put("meta", null);

        var rows = List.of(row1, row2);
        var out = temp.resolve("out.csv");

        new CsvWriter().write(out, headers, rows, CsvFormatOptions.defaults());

        var content = read(out);
        // Header
        assertTrue(content.startsWith("id,name,price,tags,active,meta"));
        // Row 1 basics
        assertTrue(content.contains("7,Paper A,12345.67"));

        // With comma delimiter, "ml; csv" may be unquoted. Accept either case by checking plain text.
        assertTrue(content.contains("ml; csv"));

        // JSON cell: normalize doubled quotes before checking
        var normalized = content.replace("\"\"", "\"");
        assertTrue(normalized.contains("{\"aid\":7,\"owner\":\"Beatriz\"}"));

        // Row 2: nulls -> empty cells (default nullString = "")
        assertTrue(content.contains("\n8,Paper B,,\"\",false,")    // tags empty string prints as ""
                || content.contains("\n8,Paper B,,,false,"));
    }


    @Test
    void customDelimiter_noHeader() throws Exception {
        var writer = new CsvWriter();
        var headers = List.of("a", "b");
        var rows = List.of(Map.of("a", "x", "b", "y"));

        var opts = new CsvFormatOptions(
                ';',                      // delimiter
                '"',                      // quote
                System.lineSeparator(),   // record separator
                org.apache.commons.csv.QuoteMode.MINIMAL,
                false,                    // printHeader = false
                "",                       // nullString
                java.nio.charset.StandardCharsets.UTF_8,
                "; ",                     // arrayJoinSeparator
                true,                     // excelSafe
                true,                     // objectInlineAsJson
                java.time.format.DateTimeFormatter.ISO_INSTANT
        );

        var out = temp.resolve("noheader.csv");
        writer.write(out, headers, rows, opts);
        var content = read(out);

        // Should NOT start with header; first line should be values with semicolon
        assertTrue(content.startsWith("x;y"));
        assertFalse(content.startsWith("a;b"));
    }

    @Test
    void nullsAndExcelSafe() throws Exception {
        var writer = new CsvWriter();

        // Use a custom null literal and ensure Excel-safe handling
        var opts = new CsvFormatOptions(
                ',', '"', System.lineSeparator(),
                org.apache.commons.csv.QuoteMode.MINIMAL,
                true,
                "NULL", // nullString
                java.nio.charset.StandardCharsets.UTF_8,
                "; ", true, true,
                java.time.format.DateTimeFormatter.ISO_INSTANT
        );

        var headers = List.of("id", "danger", "maybeNull");
        var row = new HashMap<String, Object>();
        row.put("id", 1);
        row.put("danger", "=SUM(1,2)"); // should be prefixed with '
        row.put("maybeNull", null);

        var out = temp.resolve("excel.csv");
        writer.write(out, headers, List.of(row), opts);
        var content = read(out);

        // Header present
        assertTrue(content.startsWith("id,danger,maybeNull"));
        // Excel-safe: value must be prefixed with a single quote inside CSV field
        assertTrue(content.contains(",'=SUM(1,2),NULL") || content.contains(",\"'=SUM(1,2)\",NULL"));
    }

    @Test
    void inlineObjectAsJson() throws Exception {
        var writer = new CsvWriter();
        var headers = List.of("id", "meta");

        var rows = List.of(
                Map.of("id", 1, "meta", Map.of("aid", 7, "owner", "Beatriz"))
        );

        var opts = CsvFormatOptions.defaults(); // objectInlineAsJson = true
        var out = temp.resolve("meta.csv");
        writer.write(out, headers, rows, opts);

        var content = read(out);

        assertTrue(content.startsWith("id,meta"));

        // CSV escapes inner quotes by doubling them. Normalize by collapsing "" -> "
        var normalized = content.replace("\"\"", "\"");
        assertTrue(normalized.contains("{\"aid\":7,\"owner\":\"Beatriz\"}"));
    }


    @Test
    void arraysJoined() throws Exception {
        var writer = new CsvWriter();
        var headers = List.of("tags");

        var rows = List.of(
                Map.of("tags", List.of("a", "b", "c"))
        );

        var opts = CsvFormatOptions.defaults();
        var out = temp.resolve("tags.csv");
        writer.write(out, headers, rows, opts);

        var content = read(out);
        // With comma delimiter, "a; b; c" doesn't require quotes, so just check the text exists
        assertTrue(content.contains("a; b; c"));
    }

    @Test
    void numbersAndDates() throws Exception {
        var writer = new CsvWriter();
        var headers = List.of("nInt", "nDec", "big", "when");

        var rows = List.of(
                new HashMap<>(Map.of(
                        "nInt", 42,
                        "nDec", 3.14,
                        "big", new BigDecimal("12345678901234567890.123456789"),
                        "when", Instant.parse("2025-09-29T18:45:00Z")
                ))
        );

        var opts = CsvFormatOptions.defaults(); // ISO_INSTANT for Instant
        var out = temp.resolve("nums_dates.csv");
        writer.write(out, headers, rows, opts);

        var content = read(out);
        // BigDecimal must be plain string (no scientific notation)
        assertTrue(content.contains("12345678901234567890.123456789"));
        // ISO instant
        assertTrue(content.contains("2025-09-29T18:45:00Z"));
        // integers and doubles visible
        assertTrue(content.contains("42,3.14"));
    }
}
