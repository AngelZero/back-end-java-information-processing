package ReaderAndConverter.Converter;

import ReaderAndConverter.Exceptions.CsvWriteException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * EN: Writes tabular rows ({@code Map<column, value>}) to CSV with a fixed header order.
 * Structural CSV options come from {@link CsvFormatOptions}; cell content is stringified via a
 * {@link ValueFormatter} (default: {@link DefaultValueFormatter}).
 * ES: Escribe filas tabulares ({@code Map<columna, valor>}) a CSV con un orden de encabezados fijo.
 * Las opciones estructurales provienen de {@link CsvFormatOptions}; el contenido de las celdas se
 * convierte a cadena con un {@link ValueFormatter} (por defecto: {@link DefaultValueFormatter}).
 *
 * <p><b>Example / Ejemplo</b>:</p>
 * <pre>{@code
 * var writer = new CsvWriter();
 * var headers = List.of("id","name");
 * var rows = List.of(Map.of("id",1,"name","alpha"));
 * writer.write(Path.of("out.csv"), headers, rows, CsvFormatOptions.defaults());
 * }</pre>
 */
public final class CsvWriter {
    private final ValueFormatter formatter;

    /**
     * EN: Creates a writer with a custom {@link ValueFormatter}.
     * ES: Crea un escritor con un {@link ValueFormatter} personalizado.
     */
    public CsvWriter(ValueFormatter formatter) {
        this.formatter = Objects.requireNonNull(formatter);
    }

    /** EN: Creates a writer using {@link DefaultValueFormatter}. ES: Usa {@link DefaultValueFormatter}. */
    public CsvWriter() {
        this(new DefaultValueFormatter());
    }

    /**
     * EN: Writes rows to a CSV file using the given header order and options.
     * ES: Escribe filas a un archivo CSV usando el orden de encabezados y opciones dados.
     *
     * @param output  EN: target path (created/overwritten) | ES: ruta destino (se crea/sobrescribe)
     * @param headers EN: ordered column names | ES: nombres de columnas en orden
     * @param rows    EN: rows to print; values are formatted by {@link ValueFormatter}
     *                ES: filas a imprimir; valores formateados por {@link ValueFormatter}
     * @param opts    EN/ES: CSV dialect and formatting policy | dialecto y políticas de formateo
     * @throws CsvWriteException EN/ES: if an I/O error occurs while writing | si ocurre un error de E/S
     */
    public void write(Path output,
                      List<String> headers,
                      List<? extends Map<String, ?>> rows,
                      CsvFormatOptions opts) throws CsvWriteException {

        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(headers, "headers");
        Objects.requireNonNull(rows, "rows");
        Objects.requireNonNull(opts, "opts");

        CSVFormat base = CSVFormat.Builder.create()
                .setDelimiter(opts.delimiter())
                .setQuote(opts.quote())
                .setRecordSeparator(opts.recordSeparator())
                .setQuoteMode(opts.quoteMode() == null ? QuoteMode.MINIMAL : opts.quoteMode())
                .setNullString(opts.nullString())
                .build();

        CSVFormat format = opts.printHeader()
                ? base.builder().setHeader(headers.toArray(new String[0])).build()
                : base;

        Charset enc = opts.encoding() == null ? StandardCharsets.UTF_8 : opts.encoding();

        try (BufferedWriter writer = Files.newBufferedWriter(output, enc);
             CSVPrinter printer = new CSVPrinter(writer, format)) {

            for (Map<String, ?> row : rows) {
                // EN: Build the record strictly in header order
                // ES: Construir el registro estrictamente en el orden del encabezado
                Object[] record = new Object[headers.size()];
                for (int i = 0; i < headers.size(); i++) {
                    String key = headers.get(i);
                    Object raw = row.get(key);
                    String cell = formatter.format(raw, opts);  // may return null -> prints nullString
                    record[i] = cell;
                }
                printer.printRecord(record);
            }
        } catch (IOException e) {
            throw new CsvWriteException("Failed to write CSV to: " + output, e);
        }
    }
}
