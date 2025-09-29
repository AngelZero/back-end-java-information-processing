package ReaderAndConverter.Converter;

import org.apache.commons.csv.QuoteMode;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * EN: CSV dialect configuration + cell-content formatting policy used by {@link CsvWriter}.
 * CSV is plain text; "cell formats" here means how Java values are converted to strings (dates,
 * numbers, booleans, arrays, nested objects) before printing.
 *
 * ES: Configuración del dialecto CSV + políticas de formateo del contenido de celdas usado por
 * {@link CsvWriter}. CSV es texto plano; “formatos de celda” aquí significa cómo se convierten los
 * valores Java a cadenas (fechas, números, booleanos, listas, objetos anidados) antes de imprimir.
 */
public record CsvFormatOptions(
        char delimiter,
        char quote,
        String recordSeparator,
        QuoteMode quoteMode,
        boolean printHeader,
        String nullString,              // EN: how to render nulls | ES: cómo representar los null
        Charset encoding,               // EN: file encoding | ES: codificación del archivo
        String arrayJoinSeparator,      // EN: joiner for lists | ES: separador para listas
        boolean excelSafe,              // EN: prefix to avoid Excel formulas | ES: prefijo anti-fórmulas en Excel
        boolean objectInlineAsJson,     // EN: inline nested maps as JSON | ES: inlinar mapas anidados como JSON
        DateTimeFormatter dateTimeFormatter // EN/ES: formatter for instants/dates | formateador de fechas
) {
    /**
     * EN: Returns sensible defaults (comma, minimal quoting, UTF-8, ISO-8601 dates, join arrays with "; ").
     * ES: Retorna valores por defecto (coma, “quote” mínimo, UTF-8, fechas ISO-8601, listas unidas con "; ").
     */
    public static CsvFormatOptions defaults() {
        return new CsvFormatOptions(
                ',',                     // delimiter
                '"',                     // quote
                System.lineSeparator(),  // record separator
                QuoteMode.MINIMAL,       // quote as needed
                true,                    // print header row
                "",                      // null literal => empty cell
                StandardCharsets.UTF_8,
                "; ",                    // join arrays with "; "
                true,                    // Excel-safe on
                true,                    // inline objects as JSON
                DateTimeFormatter.ISO_INSTANT
        );
    }
}
