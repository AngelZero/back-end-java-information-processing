package ReaderAndConverter.Converter;

/**
 * EN: Strategy interface that converts arbitrary Java values into CSV cell strings.
 * Return {@code null} to request the writer to emit {@code opts.nullString()} for that cell.
 *
 * ES: Interfaz de estrategia que convierte valores Java en cadenas para celdas CSV.
 * Retorna {@code null} para indicar que el escritor emita {@code opts.nullString()} en esa celda.
 */
@FunctionalInterface
public interface ValueFormatter {

    /**
     * EN: Convert a value to its final string for a CSV cell.
     * ES: Convierte un valor a su cadena final para una celda CSV.
     *
     * @param value EN/ES: value to format (may be null) | valor a formatear (puede ser null)
     * @param opts  EN/ES: formatting options | opciones de formateo
     * @return EN: the final string, or {@code null} to delegate to {@code opts.nullString()}
     *         ES: la cadena final, o {@code null} para delegar en {@code opts.nullString()}
     */
    String format(Object value, CsvFormatOptions opts);
}
