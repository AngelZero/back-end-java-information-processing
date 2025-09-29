package ReaderAndConverter.Reader;

import ReaderAndConverter.Exceptions.JsonReadException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * EN: Reads JSON content into Jackson's tree model ({@link JsonNode}).
 * <p><b>Single responsibility:</b> parsing only. Mapping/CSV emission is handled elsewhere.</p>
 *
 * ES: Lee contenido JSON en el modelo de árbol de Jackson ({@link JsonNode}).
 * <p><b>Responsabilidad única:</b> únicamente el parseo. El mapeo/emisión a CSV se maneja en otro módulo.</p>
 *
 * <p><b>Usage / Uso</b>:</p>
 * <pre>{@code
 * var reader = new JsonReader();
 * JsonNode root = reader.readTree(Path.of("data.json")); // object/array/primitive
 * }</pre>
 */
public class JsonReader {

    private final ObjectMapper mapper;

    /**
     * EN: Creates a reader using a provided {@link ObjectMapper} (custom modules, date formats, etc.).
     * ES: Crea un lector usando un {@link ObjectMapper} provisto (módulos personalizados, formatos de fecha, etc.).
     *
     * @param mapper EN: preconfigured mapper to use for parsing | ES: mapper preconfigurado para parsear
     */
    public JsonReader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * EN: Creates a reader with a default {@link ObjectMapper}.
     * ES: Crea un lector con un {@link ObjectMapper} por defecto.
     */
    public JsonReader() {
        this(new ObjectMapper());
    }

    /**
     * EN: Parses a UTF-8 JSON file into a {@link JsonNode} tree (object, array, or primitive).
     * ES: Parsea un archivo JSON UTF-8 a un árbol {@link JsonNode} (objeto, arreglo o primitivo).
     *
     * @param path EN: path to the JSON file | ES: ruta del archivo JSON
     * @return EN: parsed root node | ES: nodo raíz parseado
     * @throws JsonReadException EN: if file is missing, unreadable, or JSON is invalid
     *                           ES: si el archivo no existe, no se puede leer o el JSON es inválido
     */
    public JsonNode readTree(Path path) throws JsonReadException {
        try (InputStream in = Files.newInputStream(path)) {
            return mapper.readTree(in);
        } catch (NoSuchFileException e) {
            throw new JsonReadException("File not found: " + path, e);
        } catch (StreamReadException | JsonMappingException e) {
            throw new JsonReadException("Invalid JSON: " + path, e);
        } catch (IOException e) {
            throw new JsonReadException("I/O error reading: " + path, e);
        }
    }

    /**
     * EN: Parses a JSON file and requires the root to be an object; fails fast otherwise.
     * ES: Parsea un archivo JSON y requiere que la raíz sea un objeto; falla rápidamente si no lo es.
     *
     * @param path EN/ES: path to the JSON file | ruta del archivo JSON
     * @return EN: root as {@link ObjectNode} | ES: raíz como {@link ObjectNode}
     * @throws JsonReadException EN/ES: if root is not an object or on read errors
     */
    public ObjectNode readObjectNode(Path path) throws JsonReadException {
        JsonNode root = readTree(path);
        if (!root.isObject()) throw new JsonReadException("Expected top-level JSON object: " + path);
        return (ObjectNode) root;
    }

    /**
     * EN: Parses a JSON file and requires the root to be an array; fails fast otherwise.
     * ES: Parsea un archivo JSON y requiere que la raíz sea un arreglo; falla rápidamente si no lo es.
     *
     * @param path EN/ES: path to the JSON file | ruta del archivo JSON
     * @return EN: root as {@link ArrayNode} | ES: raíz como {@link ArrayNode}
     * @throws JsonReadException EN/ES: if root is not an array or on read errors
     */
    public ArrayNode readArrayNode(Path path) throws JsonReadException {
        JsonNode root = readTree(path);
        if (!root.isArray()) throw new JsonReadException("Expected top-level JSON array: " + path);
        return (ArrayNode) root;
    }

    /**
     * EN: Parses a JSON string (useful for unit tests or small inline configuration).
     * ES: Parsea un string JSON (útil para pruebas unitarias o configuración embebida).
     *
     * @param json EN/ES: JSON text | texto JSON
     * @return EN: parsed root node | ES: nodo raíz parseado
     * @throws JsonReadException EN/ES: if the string is not valid JSON
     */
    public JsonNode parseString(String json) throws JsonReadException {
        try {
            return mapper.readTree(json);
        } catch (StreamReadException | JsonMappingException e) {
            throw new JsonReadException("Invalid JSON string", e);
        } catch (IOException e) {
            // Unlikely for String input; kept for completeness / Poco probable con String; se incluye por completitud.
            throw new JsonReadException("I/O error parsing string", e);
        }
    }

    /**
     * EN: Reads an entire small text file as UTF-8 (helper for tests).
     * ES: Lee un archivo de texto completo como UTF-8 (ayuda para pruebas).
     *
     * @param path EN/ES: file to read | archivo a leer
     * @return EN/ES: file contents as a string | contenido del archivo como string
     * @throws UncheckedIOException EN/ES: if the file cannot be read | si no se puede leer el archivo
     */
    public static String readAll(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
