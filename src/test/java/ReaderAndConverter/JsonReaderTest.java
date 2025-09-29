package ReaderAndConverter;

import ReaderAndConverter.Exceptions.JsonReadException;
import ReaderAndConverter.Reader.JsonReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JsonReaderTest {

    private final JsonReader reader = new JsonReader();

    @TempDir
    Path temp;

    // ---------- readTree ----------

    @Test
    void readTree_parsesObjectRoot_ok() throws Exception {
        Path p = temp.resolve("obj.json");
        Files.writeString(p, "{ \"id\": 1, \"name\": \"Beatriz\", \"active\": true }");
        JsonNode root = reader.readTree(p);
        assertTrue(root.isObject());
        assertEquals(1, root.get("id").asInt());
        assertEquals("Beatriz", root.get("name").asText());
        assertTrue(root.get("active").asBoolean());
    }

    @Test
    void readTree_parsesArrayRoot_ok() throws Exception {
        Path p = temp.resolve("arr.json");
        Files.writeString(p, "[{\"id\":1},{\"id\":2}]");
        JsonNode root = reader.readTree(p);
        assertTrue(root.isArray());
        assertEquals(2, root.size());
        assertEquals(1, root.get(0).get("id").asInt());
    }

    @Test
    void readTree_malformed_throwsCustom() throws Exception {
        Path p = temp.resolve("bad.json");
        Files.writeString(p, "{ \"id\": 1"); // missing closing brace
        assertThrows(JsonReadException.class, () -> reader.readTree(p));
    }

    @Test
    void readTree_missingFile_throwsCustom() {
        Path missing = temp.resolve("nope.json");
        assertThrows(JsonReadException.class, () -> reader.readTree(missing));
    }

    // ---------- readObjectNode ----------

    @Test
    void readObjectNode_object_ok() throws Exception {
        Path p = temp.resolve("obj.json");
        Files.writeString(p, "{ \"k\": \"v\" }");
        ObjectNode obj = reader.readObjectNode(p);
        assertEquals("v", obj.get("k").asText());
    }

    @Test
    void readObjectNode_whenArray_throwsCustom() throws Exception {
        Path p = temp.resolve("arr.json");
        Files.writeString(p, "[]");
        assertThrows(JsonReadException.class, () -> reader.readObjectNode(p));
    }

    // ---------- readArrayNode ----------

    @Test
    void readArrayNode_array_ok() throws Exception {
        Path p = temp.resolve("arr.json");
        Files.writeString(p, "[1,2,3]");
        ArrayNode arr = reader.readArrayNode(p);
        assertEquals(3, arr.size());
        assertEquals(2, arr.get(1).asInt());
    }

    @Test
    void readArrayNode_whenObject_throwsCustom() throws Exception {
        Path p = temp.resolve("obj.json");
        Files.writeString(p, "{}");
        assertThrows(JsonReadException.class, () -> reader.readArrayNode(p));
    }

    // ---------- parseString ----------

    @Test
    void parseString_ok() throws Exception {
        JsonNode root = reader.parseString("{\"unicode\":\"áéí – ✓\"}");
        assertEquals("áéí – ✓", root.get("unicode").asText());
    }

    @Test
    void parseString_invalid_throwsCustom() {
        assertThrows(JsonReadException.class, () -> reader.parseString("{oops"));
    }

    // ---------- readAll ----------

    @Test
    void readAll_ok() throws Exception {
        Path p = temp.resolve("t.txt");
        Files.writeString(p, "hello");
        String s = JsonReader.readAll(p);
        assertEquals("hello", s);
    }

    @Test
    void readAll_missing_wrapsUncheckedIOException() {
        Path missing = temp.resolve("missing.txt");
        assertThrows(UncheckedIOException.class, () -> JsonReader.readAll(missing));
    }

    // ---------- edge values ----------

    @Test
    void readTree_handlesNullsBooleansNumbersAndUnicode() throws Exception {
        Path p = temp.resolve("edge.json");
        Files.writeString(p, """
            {
              "n": null,
              "b": false,
              "i": 42,
              "f": 3.14,
              "s": "café – 数据 ✓"
            }
            """);
        JsonNode root = reader.readTree(p);
        assertTrue(root.get("n").isNull());
        assertFalse(root.get("b").asBoolean());
        assertEquals(42, root.get("i").asInt());
        assertEquals(3.14, root.get("f").asDouble(), 1e-9);
        assertEquals("café – 数据 ✓", root.get("s").asText());
    }

    @Test
    void readTree_largeSimpleArray_ok() throws Exception {
        // build a quick array with 1000 numbers
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 1000; i++) {
            if (i > 0) sb.append(',');
            sb.append(i);
        }
        sb.append(']');
        Path p = temp.resolve("big.json");
        Files.writeString(p, sb.toString());

        JsonNode root = reader.readTree(p);
        assertTrue(root.isArray());
        assertEquals(1000, root.size());
        assertEquals(999, root.get(999).asInt());
    }
}
