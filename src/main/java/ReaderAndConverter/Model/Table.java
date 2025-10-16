package ReaderAndConverter.Model;

import java.util.List;
import java.util.Map;

/**
 * EN: Immutable representation of a CSV "table": a name, a fixed header order, and rows.
 * ES: Representación inmutable de una "tabla" CSV: nombre, orden de encabezados y filas.
 */
public record Table(String name, List<String> headers, List<Map<String, Object>> rows) {}
