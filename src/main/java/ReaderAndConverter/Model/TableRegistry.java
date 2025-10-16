package ReaderAndConverter.Model;

import java.util.*;

/**
 * EN: Collects rows for multiple tables and finalizes headers in a chosen order.
 * ES: Recolecta filas para múltiples tablas y fija encabezados en un orden indicado.
 */
public final class TableRegistry {

    public enum HeaderOrder { SORTED, ENCOUNTERED }

    /** Internal mutable accumulator for one table. */
    private static final class MutableTable {
        final String name;
        final LinkedHashSet<String> encounteredHeaders = new LinkedHashSet<>();
        final List<Map<String, Object>> rows = new ArrayList<>();
        MutableTable(String name) { this.name = name; }
        void addRow(Map<String, Object> row) {
            rows.add(row);
            encounteredHeaders.addAll(row.keySet());
        }
    }

    private final Map<String, MutableTable> tables = new LinkedHashMap<>();

    /** EN: Add one row to the named table (created if missing).
     *  ES: Agrega una fila a la tabla (se crea si no existe). */
    public void addRow(String tableName, Map<String, Object> row) {
        tables.computeIfAbsent(tableName, MutableTable::new).addRow(row);
    }

    /**
     * EN: Freeze tables into immutable {@link Table}s with headers in the desired order.
     * ES: Congela tablas a {@link Table}s inmutables con encabezados en el orden deseado.
     */
    public List<Table> finalizeTables(HeaderOrder order) {
        List<Table> out = new ArrayList<>();
        for (MutableTable mt : tables.values()) {
            List<String> headers = new ArrayList<>(mt.encounteredHeaders);
            if (order == HeaderOrder.SORTED) headers.sort(String::compareTo);
            // Copy rows (values remain the same references, fine for writing).
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rowsCopy = (List<Map<String, Object>>) (List<?>) new ArrayList<>(mt.rows);
            out.add(new Table(mt.name, List.copyOf(headers), rowsCopy));
        }
        return out;
    }
}
