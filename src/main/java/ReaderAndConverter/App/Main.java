package ReaderAndConverter.App;

import ReaderAndConverter.Converter.CsvFormatOptions;
import ReaderAndConverter.Converter.NormalizerConfig;
import ReaderAndConverter.Converter.NormalizerConfig.ArrayStrategy;
import ReaderAndConverter.Converter.NormalizerConfig.HeaderOrder;
import ReaderAndConverter.Converter.NormalizerConfig.PrimitiveArrayMode;
import org.apache.commons.csv.QuoteMode;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * EN: Minimal CLI that builds {@link AppConfig} from flags and delegates to {@link Application}.
 * ES: CLI mínimo que construye {@link AppConfig} a partir de banderas y delega a {@link Application}.
 *
 * Usage / Uso:
 *   java -cp app.jar ReaderAndConverter.App.Main --in samples/nested.json --out out [flags...]
 *
 * Run with --help for full list of flags.
 */
public final class Main {

    public static void main(String[] args) {
        // -------- Defaults (safe, dev-friendly) --------
        Path in  = Path.of("samples/nested.json");
        Path out = Path.of("out");

        CsvFormatOptions csv = CsvFormatOptions.defaults();      // CSV dialect + cell formatting
        NormalizerConfig norm = NormalizerConfig.defaults();      // JSON -> tables rules

        // -------- Parse args --------
        for (int i = 0; i < args.length; i++) {
            String a = args[i];

            // Basic
            switch (a) {
                case "--help", "-h" -> { printHelp(); return; }
                case "--in"  -> in  = Path.of(next(args, ++i, "--in requires a path"));
                case "--out" -> out = Path.of(next(args, ++i, "--out requires a directory"));
                default -> {
                    // CSV options
                    if (a.equals("--delimiter")) {
                        char d = requireOneChar(next(args, ++i, "--delimiter requires a single character"));
                        csv = new CsvFormatOptions(d, csv.quote(), csv.recordSeparator(), csv.quoteMode(),
                                csv.printHeader(), csv.nullString(), csv.encoding(),
                                csv.arrayJoinSeparator(), csv.excelSafe(), csv.objectInlineAsJson(),
                                csv.dateTimeFormatter());
                    } else if (a.equals("--quote")) {
                        char q = requireOneChar(next(args, ++i, "--quote requires a single character"));
                        csv = new CsvFormatOptions(csv.delimiter(), q, csv.recordSeparator(), csv.quoteMode(),
                                csv.printHeader(), csv.nullString(), csv.encoding(),
                                csv.arrayJoinSeparator(), csv.excelSafe(), csv.objectInlineAsJson(),
                                csv.dateTimeFormatter());
                    } else if (a.equals("--record-sep")) {
                        String rs = unescape(next(args, ++i, "--record-sep requires a value (e.g. \\n, \\r\\n)"));
                        csv = new CsvFormatOptions(csv.delimiter(), csv.quote(), rs, csv.quoteMode(),
                                csv.printHeader(), csv.nullString(), csv.encoding(),
                                csv.arrayJoinSeparator(), csv.excelSafe(), csv.objectInlineAsJson(),
                                csv.dateTimeFormatter());
                    } else if (a.equals("--quote-mode")) {
                        QuoteMode qm = parseQuoteMode(next(args, ++i, "--quote-mode requires minimal|all|none|non_numeric"));
                        csv = new CsvFormatOptions(csv.delimiter(), csv.quote(), csv.recordSeparator(), qm,
                                csv.printHeader(), csv.nullString(), csv.encoding(),
                                csv.arrayJoinSeparator(), csv.excelSafe(), csv.objectInlineAsJson(),
                                csv.dateTimeFormatter());
                    } else if (a.equals("--print-header")) {
                        csv = new CsvFormatOptions(csv.delimiter(), csv.quote(), csv.recordSeparator(), csv.quoteMode(),
                                true, csv.nullString(), csv.encoding(),
                                csv.arrayJoinSeparator(), csv.excelSafe(), csv.objectInlineAsJson(),
                                csv.dateTimeFormatter());
                    } else if (a.equals("--no-header")) {
                        csv = new CsvFormatOptions(csv.delimiter(), csv.quote(), csv.recordSeparator(), csv.quoteMode(),
                                false, csv.nullString(), csv.encoding(),
                                csv.arrayJoinSeparator(), csv.excelSafe(), csv.objectInlineAsJson(),
                                csv.dateTimeFormatter());
                    } else if (a.equals("--null-literal")) {
                        String lit = next(args, ++i, "--null-literal requires a string (e.g. NULL)");
                        csv = new CsvFormatOptions(csv.delimiter(), csv.quote(), csv.recordSeparator(), csv.quoteMode(),
                                csv.printHeader(), lit, csv.encoding(),
                                csv.arrayJoinSeparator(), csv.excelSafe(), csv.objectInlineAsJson(),
                                csv.dateTimeFormatter());
                    } else if (a.equals("--encoding")) {
                        Charset enc = Charset.forName(next(args, ++i, "--encoding requires a name (e.g. UTF-8)"));
                        csv = new CsvFormatOptions(csv.delimiter(), csv.quote(), csv.recordSeparator(), csv.quoteMode(),
                                csv.printHeader(), csv.nullString(), enc,
                                csv.arrayJoinSeparator(), csv.excelSafe(), csv.objectInlineAsJson(),
                                csv.dateTimeFormatter());
                    } else if (a.equals("--csv-array-join")) {
                        String sep = next(args, ++i, "--csv-array-join requires a string (e.g. \"; \")");
                        csv = new CsvFormatOptions(csv.delimiter(), csv.quote(), csv.recordSeparator(), csv.quoteMode(),
                                csv.printHeader(), csv.nullString(), csv.encoding(),
                                sep, csv.excelSafe(), csv.objectInlineAsJson(),
                                csv.dateTimeFormatter());
                    } else if (a.equals("--excel-safe")) {
                        boolean v = parseBool(next(args, ++i, "--excel-safe true|false"));
                        csv = new CsvFormatOptions(csv.delimiter(), csv.quote(), csv.recordSeparator(), csv.quoteMode(),
                                csv.printHeader(), csv.nullString(), csv.encoding(),
                                csv.arrayJoinSeparator(), v, csv.objectInlineAsJson(),
                                csv.dateTimeFormatter());
                    } else if (a.equals("--object-inline-json")) {
                        boolean v = parseBool(next(args, ++i, "--object-inline-json true|false"));
                        csv = new CsvFormatOptions(csv.delimiter(), csv.quote(), csv.recordSeparator(), csv.quoteMode(),
                                csv.printHeader(), csv.nullString(), csv.encoding(),
                                csv.arrayJoinSeparator(), csv.excelSafe(), v,
                                csv.dateTimeFormatter());
                    } else if (a.equals("--datefmt")) {
                        // pattern or keyword: ISO_INSTANT
                        String pat = next(args, ++i, "--datefmt requires a pattern or ISO_INSTANT");
                        DateTimeFormatter f = "ISO_INSTANT".equalsIgnoreCase(pat)
                                ? DateTimeFormatter.ISO_INSTANT
                                : DateTimeFormatter.ofPattern(pat, Locale.ROOT);
                        csv = new CsvFormatOptions(csv.delimiter(), csv.quote(), csv.recordSeparator(), csv.quoteMode(),
                                csv.printHeader(), csv.nullString(), csv.encoding(),
                                csv.arrayJoinSeparator(), csv.excelSafe(), csv.objectInlineAsJson(),
                                f);
                    }

                    // Normalizer options
                    else if (a.equals("--root-table")) {
                        norm = new NormalizerConfig(
                                next(args, ++i, "--root-table requires a name"),
                                norm.allowNestedTables(), norm.arrayStrategy(), norm.primitiveArrayMode(),
                                norm.addId(), norm.addParentId(), norm.idField(), norm.parentIdField(),
                                norm.joinSeparator(), norm.maxDepth(), norm.headerOrder());
                    } else if (a.equals("--allow-nested")) {
                        boolean v = parseBool(next(args, ++i, "--allow-nested true|false"));
                        norm = new NormalizerConfig(
                                norm.rootTable(), v, norm.arrayStrategy(), norm.primitiveArrayMode(),
                                norm.addId(), norm.addParentId(), norm.idField(), norm.parentIdField(),
                                norm.joinSeparator(), norm.maxDepth(), norm.headerOrder());
                    } else if (a.equals("--no-nested")) {
                        norm = new NormalizerConfig(
                                norm.rootTable(), false, norm.arrayStrategy(), norm.primitiveArrayMode(),
                                norm.addId(), norm.addParentId(), norm.idField(), norm.parentIdField(),
                                norm.joinSeparator(), norm.maxDepth(), norm.headerOrder());
                    } else if (a.equals("--array-strategy")) {
                        ArrayStrategy as = parseArrayStrategy(next(args, ++i, "--array-strategy explode|inline"));
                        norm = new NormalizerConfig(
                                norm.rootTable(), norm.allowNestedTables(), as, norm.primitiveArrayMode(),
                                norm.addId(), norm.addParentId(), norm.idField(), norm.parentIdField(),
                                norm.joinSeparator(), norm.maxDepth(), norm.headerOrder());
                    } else if (a.equals("--primitive-array")) {
                        PrimitiveArrayMode pm = parsePrimitiveArrayMode(next(args, ++i, "--primitive-array join|explode"));
                        norm = new NormalizerConfig(
                                norm.rootTable(), norm.allowNestedTables(), norm.arrayStrategy(), pm,
                                norm.addId(), norm.addParentId(), norm.idField(), norm.parentIdField(),
                                norm.joinSeparator(), norm.maxDepth(), norm.headerOrder());
                    } else if (a.equals("--add-id")) {
                        boolean v = parseBool(next(args, ++i, "--add-id true|false"));
                        norm = new NormalizerConfig(
                                norm.rootTable(), norm.allowNestedTables(), norm.arrayStrategy(), norm.primitiveArrayMode(),
                                v, norm.addParentId(), norm.idField(), norm.parentIdField(),
                                norm.joinSeparator(), norm.maxDepth(), norm.headerOrder());
                    } else if (a.equals("--add-parent-id")) {
                        boolean v = parseBool(next(args, ++i, "--add-parent-id true|false"));
                        norm = new NormalizerConfig(
                                norm.rootTable(), norm.allowNestedTables(), norm.arrayStrategy(), norm.primitiveArrayMode(),
                                norm.addId(), v, norm.idField(), norm.parentIdField(),
                                norm.joinSeparator(), norm.maxDepth(), norm.headerOrder());
                    } else if (a.equals("--no-ids")) {
                        norm = new NormalizerConfig(
                                norm.rootTable(), norm.allowNestedTables(), norm.arrayStrategy(), norm.primitiveArrayMode(),
                                false, false, norm.idField(), norm.parentIdField(),
                                norm.joinSeparator(), norm.maxDepth(), norm.headerOrder());
                    } else if (a.equals("--id-field")) {
                        norm = new NormalizerConfig(
                                norm.rootTable(), norm.allowNestedTables(), norm.arrayStrategy(), norm.primitiveArrayMode(),
                                norm.addId(), norm.addParentId(), next(args, ++i, "--id-field requires a name"), norm.parentIdField(),
                                norm.joinSeparator(), norm.maxDepth(), norm.headerOrder());
                    } else if (a.equals("--parent-id-field")) {
                        norm = new NormalizerConfig(
                                norm.rootTable(), norm.allowNestedTables(), norm.arrayStrategy(), norm.primitiveArrayMode(),
                                norm.addId(), norm.addParentId(), norm.idField(), next(args, ++i, "--parent-id-field requires a name"),
                                norm.joinSeparator(), norm.maxDepth(), norm.headerOrder());
                    } else if (a.equals("--norm-join")) {
                        String sep = next(args, ++i, "--norm-join requires a string (join for primitive arrays)");
                        norm = new NormalizerConfig(
                                norm.rootTable(), norm.allowNestedTables(), norm.arrayStrategy(), norm.primitiveArrayMode(),
                                norm.addId(), norm.addParentId(), norm.idField(), norm.parentIdField(),
                                sep, norm.maxDepth(), norm.headerOrder());
                    } else if (a.equals("--max-depth")) {
                        int md = Integer.parseInt(next(args, ++i, "--max-depth requires an integer"));
                        norm = new NormalizerConfig(
                                norm.rootTable(), norm.allowNestedTables(), norm.arrayStrategy(), norm.primitiveArrayMode(),
                                norm.addId(), norm.addParentId(), norm.idField(), norm.parentIdField(),
                                norm.joinSeparator(), md, norm.headerOrder());
                    } else if (a.equals("--header-order")) {
                        HeaderOrder ho = parseHeaderOrder(next(args, ++i, "--header-order sorted|encountered"));
                        norm = new NormalizerConfig(
                                norm.rootTable(), norm.allowNestedTables(), norm.arrayStrategy(), norm.primitiveArrayMode(),
                                norm.addId(), norm.addParentId(), norm.idField(), norm.parentIdField(),
                                norm.joinSeparator(), norm.maxDepth(), ho);
                    } else {
                        // unknown flag: ignore silently to keep CLI simple
                    }
                }
            }
        }

        try {
            new Application().run(new AppConfig(in, out, csv, norm));
            System.out.println("CSV written to: " + out.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    // -------- Helpers --------

    private static String next(String[] args, int idx, String err) {
        if (idx >= args.length) throw new IllegalArgumentException(err);
        return args[idx];
    }

    private static char requireOneChar(String s) {
        if (s == null || s.length() != 1) throw new IllegalArgumentException("Expected a single character, got: " + s);
        return s.charAt(0);
    }

    /** Unescape \n, \r, \t, and \r\n for record separator convenience. */
    private static String unescape(String s) {
        return s.replace("\\r\\n", "\r\n")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    private static boolean parseBool(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "true", "1", "yes", "y" -> true;
            case "false", "0", "no", "n" -> false;
            default -> throw new IllegalArgumentException("Expected boolean, got: " + s);
        };
    }

    private static QuoteMode parseQuoteMode(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "minimal" -> QuoteMode.MINIMAL;
            case "all" -> QuoteMode.ALL;
            case "none" -> QuoteMode.NONE;
            case "non_numeric", "non-numeric" -> QuoteMode.NON_NUMERIC;
            default -> throw new IllegalArgumentException("Unknown quote-mode: " + s);
        };
    }

    private static ArrayStrategy parseArrayStrategy(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "explode" -> ArrayStrategy.EXPLODE_TO_CHILD;
            case "inline"  -> ArrayStrategy.INLINE_AS_JSON;
            default -> throw new IllegalArgumentException("Unknown array-strategy: " + s);
        };
    }

    private static PrimitiveArrayMode parsePrimitiveArrayMode(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "join"    -> PrimitiveArrayMode.JOIN;
            case "explode" -> PrimitiveArrayMode.EXPLODE_TO_CHILD;
            default -> throw new IllegalArgumentException("Unknown primitive-array mode: " + s);
        };
    }

    private static HeaderOrder parseHeaderOrder(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "sorted"      -> HeaderOrder.SORTED;
            case "encountered" -> HeaderOrder.ENCOUNTERED;
            default -> throw new IllegalArgumentException("Unknown header-order: " + s);
        };
    }

    private static void printHelp() {
        System.out.println("""
        Usage:
          java -cp <jar> ReaderAndConverter.App.Main --in <json> --out <dir> [options]

        Basic:
          --in <path>                 Input JSON file
          --out <dir>                 Output directory
          --help                      Show this help

        CSV dialect (CsvFormatOptions):
          --delimiter <char>          e.g. , ; | \t
          --quote <char>              e.g. " '
          --record-sep <str>          e.g. \\n, \\r\\n, or any string
          --quote-mode <mode>         minimal | all | none | non_numeric
          --print-header              Print header row (default: on)
          --no-header                 Do not print header row
          --null-literal <str>        e.g. NULL (default: empty cell)
          --encoding <name>           e.g. UTF-8, ISO-8859-1 (default: UTF-8)
          --csv-array-join <str>      Join for collections inside one cell (default: "; ")
          --excel-safe <bool>         true|false prefix ' to =+-@ (default: true)
          --object-inline-json <bool> true|false inline maps as JSON (default: true)
          --datefmt <pattern|ISO_INSTANT>  Formatter for Instant/ZonedDateTime

        Normalizer (NormalizerConfig):
          --root-table <name>         Root table/file name (default: root)
          --allow-nested <bool>       true|false (default: true)
          --no-nested                 Shorthand for --allow-nested false
          --array-strategy <opt>      explode | inline     (arrays of objects)
          --primitive-array <opt>     join | explode       (arrays of primitives)
          --add-id <bool>             Add incremental id per table (default: true)
          --add-parent-id <bool>      Add parent_id in child tables (default: true)
          --no-ids                    Shorthand for both above = false
          --id-field <name>           id column name (default: id)
          --parent-id-field <name>    parent id column (default: parent_id)
          --norm-join <str>           Join for primitive arrays (default: "; ")
          --max-depth <int>           Traverse depth guard (default: 64)
          --header-order <opt>        sorted | encountered (default: sorted)
        """);
    }
}
