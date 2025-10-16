package ReaderAndConverter.Converter;

/**
 * EN: Options controlling JSON → tables normalization.
 * ES: Opciones que controlan la normalización JSON → tablas.
 */
public record NormalizerConfig(
        String rootTable,
        boolean allowNestedTables,
        ArrayStrategy arrayStrategy,           // for arrays of objects
        PrimitiveArrayMode primitiveArrayMode, // for arrays of primitives
        boolean addId,
        boolean addParentId,
        String idField,
        String parentIdField,
        String joinSeparator,
        int maxDepth,
        HeaderOrder headerOrder
) {
    /** EN/ES: Array-of-objects behavior | Comportamiento para arreglos de objetos */
    public enum ArrayStrategy { EXPLODE_TO_CHILD, INLINE_AS_JSON }
    /** EN/ES: Array-of-primitives behavior | Comportamiento para arreglos de primitivos */
    public enum PrimitiveArrayMode { JOIN, EXPLODE_TO_CHILD }
    /** EN/ES: Header order | Orden de encabezados */
    public enum HeaderOrder { SORTED, ENCOUNTERED }

    public static NormalizerConfig defaults() {
        return new NormalizerConfig(
                "root",
                true,
                ArrayStrategy.EXPLODE_TO_CHILD,
                PrimitiveArrayMode.JOIN,
                true,
                true,
                "id",
                "parent_id",
                "; ",
                64,
                HeaderOrder.SORTED
        );
    }
}
