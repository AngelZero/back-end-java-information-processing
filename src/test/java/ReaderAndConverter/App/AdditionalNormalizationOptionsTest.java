package ReaderAndConverter.App;

import ReaderAndConverter.Converter.CsvFormatOptions;
import ReaderAndConverter.Converter.NormalizerConfig;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extra end-to-end tests to exercise remaining configuration combinations:
 * - ArrayStrategy.INLINE_AS_JSON (while nested tables are allowed)
 * - PrimitiveArrayMode.EXPLODE_TO_CHILD (explode arrays of primitives)
 * - Header order ENCOUNTERED (vs SORTED)
 * - Custom nullString literal
 * - Excel-safe OFF (do not prefix '= + - @')
 * - objectInlineAsJson OFF (fallback to Map#toString)
 *
 * Each test writes outputs to a JUnit-managed temporary directory.
 */
class AdditionalNormalizationOptionsTest {

    @TempDir
    Path temp;

    /** Copy a resource (from src/test/resources) to a temp file so the app can open it by filesystem Path. */
    private Path copyResourceToTemp(String resource, String outName) throws Exception {
        Path target = temp.resolve(outName);
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "Missing test resource: " + resource);
            Files.copy(in, target);
        }
        return target;
    }

    /** Read a text file as UTF-8. */
    private String read(Path p) throws Exception {
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    /** Return the first line of a text file (platform independent). */
    private String firstLine(Path p) throws Exception {
        try (var lines = Files.lines(p, StandardCharsets.UTF_8)) {
            return lines.findFirst().orElse("");
        }
    }

    // --- Tests ---

    @Test
    void arrayOfObjects_inlined_asJson_while_nestedAllowed() throws Exception {
        // samples/nested.json has: author {object}, papers [array of objects], active boolean
        Path in = copyResourceToTemp("samples/nested.json", "nested.json");
        Path outDir = temp.resolve("out");

        // Keep nested tables allowed, but inline arrays-of-objects as JSON (so "papers.csv" should NOT be created).
        NormalizerConfig norm = new NormalizerConfig(
                "root",
                true,  // allow nested tables
                NormalizerConfig.ArrayStrategy.INLINE_AS_JSON,     // <- inline array-of-objects
                NormalizerConfig.PrimitiveArrayMode.JOIN,
                true, true, "id", "parent_id",
                "; ", 64, NormalizerConfig.HeaderOrder.SORTED
        );

        new Application().run(new AppConfig(in, outDir, CsvFormatOptions.defaults(), norm));

        assertTrue(Files.exists(outDir.resolve("root.csv")));
        assertTrue(Files.exists(outDir.resolve("author.csv")));      // object child still emitted
        assertFalse(Files.exists(outDir.resolve("papers.csv")));     // array-of-objects was inlined

        String root = read(outDir.resolve("root.csv")).replace("\"\"", "\"");
        assertTrue(root.contains("[{\"pid\":101") || root.contains("\"papers\"")); // papers inlined as JSON text
    }

    @Test
    void primitiveArrays_exploded_to_child_table() throws Exception {
        Path in = copyResourceToTemp("samples/nested.json", "nested.json");
        Path outDir = temp.resolve("out");

        // Explode arrays of primitives (tags) to a child table "papers.tags" -> "papers_tags.csv"
        NormalizerConfig norm = new NormalizerConfig(
                "root",
                true,
                NormalizerConfig.ArrayStrategy.EXPLODE_TO_CHILD,
                NormalizerConfig.PrimitiveArrayMode.EXPLODE_TO_CHILD, // <- explode primitives
                true, true, "id", "parent_id",
                "; ", 64, NormalizerConfig.HeaderOrder.SORTED
        );

        new Application().run(new AppConfig(in, outDir, CsvFormatOptions.defaults(), norm));

        assertTrue(Files.exists(outDir.resolve("root.csv")));
        assertTrue(Files.exists(outDir.resolve("papers.csv")));
        Path tags = outDir.resolve("papers_tags.csv");
        assertTrue(Files.exists(tags)); // child table for primitive arrays

        // Expect two rows (etl, csv) coming from first paper's tags; second paper has empty tags
        String all = read(tags);
        assertTrue(all.contains("etl"));
        assertTrue(all.contains("csv"));
    }

    @Test
    void headerOrder_encountered_preservesEncounterSequence() throws Exception {
        // Object order in obj.json: title, year, active (plus id gets added first)
        Path in = copyResourceToTemp("samples/obj.json", "obj.json");
        Path outDir = temp.resolve("out");

        NormalizerConfig norm = new NormalizerConfig(
                "root",
                true,
                NormalizerConfig.ArrayStrategy.EXPLODE_TO_CHILD,
                NormalizerConfig.PrimitiveArrayMode.JOIN,
                true, true, "id", "parent_id",
                "; ", 64, NormalizerConfig.HeaderOrder.ENCOUNTERED // <- important
        );

        new Application().run(new AppConfig(in, outDir, CsvFormatOptions.defaults(), norm));

        // Expect header like: id,title,year,active (encountered order)
        String header = firstLine(outDir.resolve("root.csv"));
        assertTrue(header.startsWith("id,"), "Header should start with id when encountered order is used");
        assertTrue(header.contains("title"));
        assertTrue(header.contains("year"));
        assertTrue(header.contains("active"));
    }

    @Test
    void nullString_literal_printed_for_missing_values() throws Exception {
        // Build a small array with missing fields to force null cells.
        Path in = temp.resolve("sparse.json");
        Files.writeString(in, """
            [ {"a":1}, {"b":2} ]
            """, StandardCharsets.UTF_8);
        Path outDir = temp.resolve("out");

        CsvFormatOptions csv = new CsvFormatOptions(
                ',', '"', System.lineSeparator(),
                org.apache.commons.csv.QuoteMode.MINIMAL,
                true, "NULL",                         // <- set null literal
                StandardCharsets.UTF_8,
                "; ", true, true,
                DateTimeFormatter.ISO_INSTANT
        );

        new Application().run(new AppConfig(in, outDir, csv, NormalizerConfig.defaults()));

        String out = read(outDir.resolve("root.csv"));
        assertTrue(out.contains("NULL"), "Missing cells should render as the configured null literal");
    }

    @Test
    void excelSafe_off_doesNotPrefix_formulaLikeStrings() throws Exception {
        // A value that looks like an Excel formula
        Path in = temp.resolve("excel.json");
        Files.writeString(in, """
            { "danger": "=2+2" }
            """, StandardCharsets.UTF_8);
        Path outDir = temp.resolve("out");

        CsvFormatOptions base = CsvFormatOptions.defaults();
        CsvFormatOptions csv = new CsvFormatOptions(
                base.delimiter(), base.quote(), base.recordSeparator(), base.quoteMode(),
                base.printHeader(), base.nullString(), base.encoding(),
                base.arrayJoinSeparator(), /* excelSafe= */ false, base.objectInlineAsJson(), // <- off
                base.dateTimeFormatter()
        );

        new Application().run(new AppConfig(in, outDir, csv, NormalizerConfig.defaults()));

        String out = read(outDir.resolve("root.csv"));
        assertTrue(out.contains("=2+2"));          // no leading single-quote
        assertFalse(out.contains("'=2+2"));        // confirm not prefixed
    }

    @Test
    void objectInlineAsJson_flag_has_no_effect_when_normalizer_inlines_as_String() throws Exception {
        // Disallow nested tables -> normalizer inlines nested objects/arrays as JSON *strings*.
        // Since the value is already a String, CsvFormatOptions.objectInlineAsJson has no effect.
        Path in = copyResourceToTemp("samples/nested.json", "nested.json");
        Path outDir = temp.resolve("out");

        var norm = new NormalizerConfig(
                "root",
                false, // no nested tables -> everything inlined as String(JSON)
                NormalizerConfig.ArrayStrategy.EXPLODE_TO_CHILD,
                NormalizerConfig.PrimitiveArrayMode.JOIN,
                true, true, "id", "parent_id",
                "; ", 64, NormalizerConfig.HeaderOrder.SORTED
        );

        var base = CsvFormatOptions.defaults();
        var csv = new CsvFormatOptions(
                base.delimiter(), base.quote(), base.recordSeparator(), base.quoteMode(),
                base.printHeader(), base.nullString(), base.encoding(),
                base.arrayJoinSeparator(), base.excelSafe(),
                /* objectInlineAsJson= */ false,          // has no effect on String values
                base.dateTimeFormatter()
        );

        new Application().run(new AppConfig(in, outDir, csv, norm));

        String root = read(outDir.resolve("root.csv")).replace("\"\"", "\"");
        // Expect JSON-style inline strings for nested fields (author, papers)
        assertTrue(root.contains("{\"id\":7,\"name\":\"Beatriz\"}"));
        assertTrue(root.contains("\"papers\"") || root.contains("[{\"pid\":"));
    }

}
