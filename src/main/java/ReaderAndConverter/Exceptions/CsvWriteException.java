package ReaderAndConverter.Exceptions;

import java.io.IOException;

/**
 * EN: Checked exception thrown when a CSV file cannot be created or written.
 * ES: Excepción verificada lanzada cuando un archivo CSV no puede crearse o escribirse.
 */
public class CsvWriteException extends IOException {
    /** EN/ES: message + cause | mensaje + causa */
    public CsvWriteException(String message, Throwable cause) { super(message, cause); }
    /** EN/ES: message only | solo mensaje */
    public CsvWriteException(String message) { super(message); }
}
