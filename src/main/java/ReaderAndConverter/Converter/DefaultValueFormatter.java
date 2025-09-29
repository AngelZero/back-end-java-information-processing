package ReaderAndConverter.Converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.Map;

/**
 * EN: Default {@link ValueFormatter} implementation.
 * <ul>
 *   <li>Numbers: locale-neutral (dot decimal); {@link BigDecimal} via {@code toPlainString()}.</li>
 *   <li>Booleans: {@code true}/{@code false}.</li>
 *   <li>Dates/times: ISO via {@link CsvFormatOptions#dateTimeFormatter()} (Instant/Zoned/Offset).</li>
 *   <li>Collections: join using {@link CsvFormatOptions#arrayJoinSeparator()}.</li>
 *   <li>Maps: inline as compact JSON when {@link CsvFormatOptions#objectInlineAsJson()} is true.</li>
 *   <li>Excel safety: prefix strings starting with {@code =,+,-,@} with a single quote when enabled.</li>
 * </ul>
 *
 * ES: Implementación por defecto de {@link ValueFormatter}.
 * <ul>
 *   <li>Números: sin localidad (punto decimal); {@link BigDecimal} con {@code toPlainString()}.</li>
 *   <li>Booleanos: {@code true}/{@code false}.</li>
 *   <li>Fechas/horas: ISO usando {@link CsvFormatOptions#dateTimeFormatter()}.</li>
 *   <li>Colecciones: unión con {@link CsvFormatOptions#arrayJoinSeparator()}.</li>
 *   <li>Mapas: en línea como JSON compacto cuando {@link CsvFormatOptions#objectInlineAsJson()} es true.</li>
 *   <li>Seguridad Excel: prefijo comilla simple si inicia con {@code =,+,-,@} cuando está habilitado.</li>
 * </ul>
 */
public final class DefaultValueFormatter implements ValueFormatter {
    private static final ObjectMapper OM = new ObjectMapper();

    /**
     * EN: Convert a value to its final CSV string representation.
     * ES: Convierte un valor a su representación final de cadena en CSV.
     *
     * @param value EN/ES: value to format (may be null) | valor a formatear (puede ser null)
     * @param opts  EN/ES: formatting options | opciones de formateo
     * @return EN: final string, or {@code null} to let the writer emit {@code opts.nullString()}
     *         ES: cadena final, o {@code null} para que el writer emita {@code opts.nullString()}
     */
    @Override
    public String format(Object value, CsvFormatOptions opts) {
        if (value == null) return null; // writer prints opts.nullString()

        // Strings (Excel-safe handling)
        if (value instanceof CharSequence s) {
            String str = s.toString();
            return opts.excelSafe() && startsLikeExcelFormula(str) ? "'" + str : str;
        }

        // Numbers
        if (value instanceof BigDecimal bd) return bd.toPlainString();
        if (value instanceof Number n)      return String.valueOf(n);

        // Java Time
        if (value instanceof Instant i)         return opts.dateTimeFormatter().format(i);
        if (value instanceof ZonedDateTime zdt) return opts.dateTimeFormatter().format(zdt.toInstant());
        if (value instanceof OffsetDateTime odt)return opts.dateTimeFormatter().format(odt.toInstant());
        if (value instanceof LocalDateTime ldt) return ldt.toString();
        if (value instanceof LocalDate ld)      return ld.toString();
        if (value instanceof TemporalAccessor ta) return ta.toString();

        // Collections
        if (value instanceof Collection<?> col) {
            return col.stream()
                    .map(elem -> {
                        String s = format(elem, opts);
                        return s == null ? opts.nullString() : s;
                    })
                    .reduce((a, b) -> a + opts.arrayJoinSeparator() + b)
                    .orElse("");
        }

        // Maps / arbitrary objects
        if (value instanceof Map<?, ?> map && opts.objectInlineAsJson()) {
            try {
                return OM.writeValueAsString(map);
            } catch (JsonProcessingException ignored) {
                // fall through to default toString()
            }
        }
        return String.valueOf(value);
    }

    private static boolean startsLikeExcelFormula(String s) {
        if (s.isEmpty()) return false;
        char c = s.charAt(0);
        return c == '=' || c == '+' || c == '-' || c == '@';
    }
}
