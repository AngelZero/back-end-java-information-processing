package ReaderAndConverter.Converter;

import ReaderAndConverter.Model.Table;
import ReaderAndConverter.Model.TableRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;

/**
 * EN: Walks a {@link JsonNode} tree and produces one or more CSV "tables"
 *      based on {@link NormalizerConfig}. Supports child tables, IDs, and parent IDs.
 *
 * ES: Recorre un árbol {@link JsonNode} y produce una o varias "tablas" CSV
 *      según {@link NormalizerConfig}. Soporta tablas hijas, IDs y parent IDs.
 */
public final class JsonNormalizer {

    private final NormalizerConfig cfg;
    private final TableRegistry registry = new TableRegistry();
    private final Map<String, Long> idCounters = new HashMap<>();

    public JsonNormalizer(NormalizerConfig cfg) {
        this.cfg = cfg;
    }

    /** EN/ES: Normalize a parsed JSON root into CSV tables | Normaliza un JSON raíz a tablas CSV */
    public List<Table> normalize(JsonNode root) {
        if (root == null || root.isNull()) {
            return List.of(new Table(cfg.rootTable(), List.of(), List.of()));
        }
        if (root.isArray()) {
            normalizeArray((ArrayNode) root, cfg.rootTable(), null, "");
        } else if (root.isObject()) {
            normalizeObject((ObjectNode) root, cfg.rootTable(), null, "");
        } else {
            // primitive root: put as a single column "value"
            Map<String, Object> row = baseRow(cfg.rootTable(), null);
            row.put("value", primitiveValue(root));
            registry.addRow(cfg.rootTable(), row);
        }
        var headerOrder = cfg.headerOrder() == NormalizerConfig.HeaderOrder.SORTED
                ? TableRegistry.HeaderOrder.SORTED
                : TableRegistry.HeaderOrder.ENCOUNTERED;
        return registry.finalizeTables(headerOrder);
    }

    // ---------- Core traversal ----------

    private void normalizeObject(ObjectNode obj, String tableName, Long parentId, String path) {
        long myId = cfg.addId() ? nextId(tableName) : -1L;
        Map<String, Object> row = baseRow(tableName, parentId);
        if (cfg.addId()) row.put(cfg.idField(), myId);

        // First pass: primitives into current row
        obj.fieldNames().forEachRemaining(fn -> {
            JsonNode child = obj.get(fn);
            if (isPrimitive(child)) {
                row.put(col(path, fn), primitiveValue(child));
            }
        });

        // Second pass: objects/arrays
        obj.fieldNames().forEachRemaining(fn -> {
            JsonNode child = obj.get(fn);
            if (isPrimitive(child)) return;
            String childPath = path.isEmpty() ? fn : path + "." + fn;

            if (child.isObject()) {
                if (!cfg.allowNestedTables()) {
                    row.put(col(path, fn), child.toString());
                } else {
                    normalizeObject((ObjectNode) child, tableNameFor(childPath),
                            cfg.addId() ? myId : parentId, childPath);
                }
            } else if (child.isArray()) {
                ArrayNode arr = (ArrayNode) child;
                if (arr.size() == 0) {
                    row.put(col(path, fn), "");
                } else if (arr.get(0).isObject()) {
                    if (cfg.arrayStrategy() == NormalizerConfig.ArrayStrategy.INLINE_AS_JSON || !cfg.allowNestedTables()) {
                        row.put(col(path, fn), arr.toString());
                    } else {
                        normalizeArray(arr, tableNameFor(childPath),
                                cfg.addId() ? myId : parentId, childPath);
                    }
                } else {
                    // primitives array
                    if (cfg.primitiveArrayMode() == NormalizerConfig.PrimitiveArrayMode.JOIN || !cfg.allowNestedTables()) {
                        row.put(col(path, fn), joinPrimitiveArray(arr));
                    } else {
                        normalizePrimitiveArrayToChild(arr, tableNameFor(childPath),
                                cfg.addId() ? myId : parentId, childPath);
                    }
                }
            }
        });

        registry.addRow(tableName, row);
    }

    private void normalizeArray(ArrayNode arr, String tableName, Long parentId, String path) {
        for (JsonNode elem : arr) {
            if (elem.isObject()) {
                normalizeObject((ObjectNode) elem, tableName, parentId, path);
            } else if (elem.isArray()) {
                Map<String, Object> row = baseRow(tableName, parentId);
                if (cfg.addId()) row.put(cfg.idField(), nextId(tableName));
                row.put(col(path, "value"), elem.toString());
                registry.addRow(tableName, row);
            } else {
                Map<String, Object> row = baseRow(tableName, parentId);
                if (cfg.addId()) row.put(cfg.idField(), nextId(tableName));
                row.put(col(path, "value"), primitiveValue(elem));
                registry.addRow(tableName, row);
            }
        }
    }

    private void normalizePrimitiveArrayToChild(ArrayNode arr, String tableName, Long parentId, String path) {
        for (JsonNode elem : arr) {
            Map<String, Object> row = baseRow(tableName, parentId);
            if (cfg.addId()) row.put(cfg.idField(), nextId(tableName));
            row.put(col(path, "value"), primitiveValue(elem));
            registry.addRow(tableName, row);
        }
    }

    // ---------- Helpers ----------

    private Map<String, Object> baseRow(String tableName, Long parentId) {
        Map<String, Object> row = new LinkedHashMap<>();
        if (cfg.addParentId() && parentId != null) {
            row.put(cfg.parentIdField(), parentId);
        }
        return row;
    }

    private boolean isPrimitive(JsonNode n) {
        return n == null || n.isNull() || n.isNumber() || n.isTextual() || n.isBoolean();
    }

    private Object primitiveValue(JsonNode n) {
        if (n == null || n.isNull()) return null;
        if (n.isNumber()) return n.numberValue();
        if (n.isBoolean()) return n.booleanValue();
        return n.asText();
    }

    private String joinPrimitiveArray(ArrayNode arr) {
        String sep = cfg.joinSeparator();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.size(); i++) {
            if (i > 0) sb.append(sep);
            sb.append(String.valueOf(primitiveValue(arr.get(i))));
        }
        return sb.toString();
    }

    private String tableNameFor(String path) {
        return path.replace('.', '_');
    }

    private String col(String path, String field) {
        return path.isEmpty() ? field : path + "." + field;
    }

    private long nextId(String table) {
        long next = idCounters.getOrDefault(table, 0L) + 1;
        idCounters.put(table, next);
        return next;
    }
}
