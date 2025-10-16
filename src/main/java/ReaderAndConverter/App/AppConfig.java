package ReaderAndConverter.App;

import ReaderAndConverter.Converter.CsvFormatOptions;
import ReaderAndConverter.Converter.NormalizerConfig;

import java.nio.file.Path;

/**
 * EN: Aggregates all runtime parameters for the pipeline: input, output, CSV dialect, normalization.
 * ES: Agrega todos los parámetros de ejecución: entrada, salida, dialecto CSV, normalización.
 */
public record AppConfig(
        Path inputJson,
        Path outputDir,
        CsvFormatOptions csvOptions,
        NormalizerConfig normOptions
) {
    public static AppConfig withDefaults(Path inputJson, Path outputDir) {
        return new AppConfig(inputJson, outputDir, CsvFormatOptions.defaults(), NormalizerConfig.defaults());
    }
}
