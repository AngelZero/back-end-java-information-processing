package ReaderAndConverter.App;

import ReaderAndConverter.Converter.*;
import ReaderAndConverter.Exceptions.CsvWriteException;
import ReaderAndConverter.Exceptions.JsonReadException;
import ReaderAndConverter.Model.Table;
import ReaderAndConverter.Reader.JsonReader;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * EN: Orchestrates the whole pipeline: read JSON → normalize → write CSVs.
 * ES: Orquesta el pipeline completo: leer JSON → normalizar → escribir CSVs.
 */
public final class Application {

    private final JsonReader reader = new JsonReader();
    private final CsvWriter writer = new CsvWriter();

    /**
     * EN: Executes the transformation and writes one or more CSV files to {@code outputDir}.
     * ES: Ejecuta la transformación y escribe uno o más CSVs en {@code outputDir}.
     *
     * @throws IOException EN/ES: if I/O errors occur during read/write | si ocurren errores de E/S
     */
    public void run(AppConfig cfg) throws IOException {
        // ensure output dir
        Files.createDirectories(cfg.outputDir());

        // 1) Read
        JsonNode root = reader.readTree(cfg.inputJson());

        // 2) Normalize
        JsonNormalizer normalizer = new JsonNormalizer(cfg.normOptions());
        List<Table> tables = normalizer.normalize(root);

        // 3) Write CSV(s)
        for (Table t : tables) {
            Path out = cfg.outputDir().resolve(t.name() + ".csv");
            writer.write(out, t.headers(), t.rows(), cfg.csvOptions());
        }
    }
}
