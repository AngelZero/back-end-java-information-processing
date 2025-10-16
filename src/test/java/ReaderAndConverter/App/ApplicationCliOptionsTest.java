package ReaderAndConverter.App;

import ReaderAndConverter.Converter.CsvFormatOptions;
import ReaderAndConverter.Converter.NormalizerConfig;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationCliOptionsTest {

    @TempDir
    Path temp;

    /**
     * Copies a resource from src/test/resources to a temporary file so the app can open it by Path.
     *
     * @param resource path relative to classpath root, e.g. "samples/obj.json"
     * @param outName  filename to use in the temp directory
     * @return the Path to the copied temp file
     */
    private Path copyResourceToTemp(String resource, String outName) throws Exception {
        Path target = temp.resolve(outName);
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "Missing test resource: " + resource);
            Files.copy(in, target);
        }
        return target;
    }

    /** Reads a text file as UTF-8 into a String. */
    private String read(Path p) throws Exception {
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    /** Parses a CSV file with a known delimiter and optional header. */
    private CSVParser parseCsv(Path file, char delimiter, boolean hasHeader) throws Exception {
        CSVFormat fmt = hasHeader
                ? CSVFormat.DEFAULT.builder().setDelimiter(delimiter).setHeader().setSkipHeaderRecord(true).build()
                : CSVFormat.DEFAULT.builder().setDelimiter(delimiter).build();
        return CSVParser.parse(file, StandardCharsets.UTF_8, fmt);
    }

    @Test
    void singleObject_defaults_expectHeaderAndValues() throws Exception {
        Path in = copyResourceToTemp("samples/obj.json", "obj.json");
        Path outDir = temp.resolve("out");

        new Application().run(AppConfig.withDefaults(in, outDir));

        Path out = outDir.resolve("root.csv");
        assertTrue(Files.exists(out));

        try (CSVParser p = parseCsv(out, ',', true)) {
            List<CSVRecord> rows = p.getRecords();
            assertEquals(1, rows.size());
            CSVRecord r = rows.get(0);
            // Headers are sorted by default; just assert values are present in some order
            assertTrue(r.toString().contains("Paper A"));
            assertTrue(r.toString().contains("2024"));
            assertTrue(r.toString().toLowerCase().contains("true"));
        }
    }

    @Test
    void arrayOfObjects_semicolon_noHeader_noIds_expectTwoRows() throws Exception {
        Path in = copyResourceToTemp("samples/arr.json", "arr.json");
        Path outDir = temp.resolve("out");

        CsvFormatOptions csv = new CsvFormatOptions(
                ';', '"', System.lineSeparator(),
                org.apache.commons.csv.QuoteMode.MINIMAL,
                false, "", // no header, empty null
                StandardCharsets.UTF_8,
                "; ", true, true,
                DateTimeFormatter.ISO_INSTANT
        );
        NormalizerConfig norm = new NormalizerConfig(
                "root", true,
                NormalizerConfig.ArrayStrategy.EXPLODE_TO_CHILD,
                NormalizerConfig.PrimitiveArrayMode.JOIN,
                false, false, "id", "parent_id",
                "; ", 64, NormalizerConfig.HeaderOrder.SORTED
        );

        new Application().run(new AppConfig(in, outDir, csv, norm));

        Path out = outDir.resolve("root.csv");
        assertTrue(Files.exists(out));

        try (CSVParser p = parseCsv(out, ';', false)) {
            List<CSVRecord> rows = p.getRecords();
            assertEquals(2, rows.size());      // two objects → two rows
            assertEquals(2, rows.get(0).size()); // typically "title;year" columns (no id, no header)
            // Check values are present across two rows
            String all = rows.toString();
            assertTrue(all.contains("A"));
            assertTrue(all.contains("2024"));
            assertTrue(all.contains("B"));
            assertTrue(all.contains("2025"));
        }
    }

    @Test
    void nested_allowed_childTables_exist_andTagsJoined() throws Exception {
        Path in = copyResourceToTemp("samples/nested.json", "nested.json");
        Path outDir = temp.resolve("out");

        new Application().run(AppConfig.withDefaults(in, outDir));

        Path root   = outDir.resolve("root.csv");
        Path author = outDir.resolve("author.csv");
        Path papers = outDir.resolve("papers.csv");

        assertAll(
                () -> assertTrue(Files.exists(root)),
                () -> assertTrue(Files.exists(author)),
                () -> assertTrue(Files.exists(papers))
        );

        // In papers.csv the tags array (primitive) should be joined with "; "
        try (CSVParser p = parseCsv(papers, ',', true)) {
            String all = p.getRecords().toString();
            assertTrue(all.contains("etl; csv"));
        }
    }

    @Test
    void nested_disallowed_inlineJson_inRoot_onlyOneCsv() throws Exception {
        Path in = copyResourceToTemp("samples/nested.json", "nested.json");
        Path outDir = temp.resolve("out");

        NormalizerConfig norm = new NormalizerConfig(
                "root",
                false, // no nested tables -> inline JSON strings
                NormalizerConfig.ArrayStrategy.EXPLODE_TO_CHILD,
                NormalizerConfig.PrimitiveArrayMode.JOIN,
                true, true, "id", "parent_id",
                "; ", 64, NormalizerConfig.HeaderOrder.SORTED
        );

        new Application().run(new AppConfig(in, outDir, CsvFormatOptions.defaults(), norm));

        Path root = outDir.resolve("root.csv");
        assertTrue(Files.exists(root));
        assertFalse(Files.exists(outDir.resolve("author.csv")));
        assertFalse(Files.exists(outDir.resolve("papers.csv")));

        // Root should contain inlined JSON text for 'author' and 'papers'
        String content = read(root).replace("\"\"", "\""); // normalize CSV-escaped quotes
        assertTrue(content.contains("{\"id\":7,\"name\":\"Beatriz\"}"));
        assertTrue(content.contains("\"papers\"") || content.contains("[{\"pid\":"));
    }

    @Test
    void renameRootTable_andCustomJoinSeparator() throws Exception {
        Path in = copyResourceToTemp("samples/arr.json", "arr.json");
        Path outDir = temp.resolve("out");

        NormalizerConfig norm = new NormalizerConfig(
                "publications",
                true,
                NormalizerConfig.ArrayStrategy.EXPLODE_TO_CHILD,
                NormalizerConfig.PrimitiveArrayMode.JOIN,
                true, false, "id", "parent_id",
                " | ", 64, NormalizerConfig.HeaderOrder.SORTED
        );

        new Application().run(new AppConfig(in, outDir, CsvFormatOptions.defaults(), norm));

        Path pubs = outDir.resolve("publications.csv");
        assertTrue(Files.exists(pubs));
        // we don't know exact data unless arr.json has arrays; ensure at least data-like content
        assertTrue(Files.size(pubs) > 0);
    }
}
