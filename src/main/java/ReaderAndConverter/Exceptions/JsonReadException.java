package ReaderAndConverter.Exceptions;

import java.io.IOException;

/**
 * EN: Checked exception representing any failure while reading or parsing JSON.
 * Wrapping I/O and Jackson exceptions behind a single type gives callers a stable API
 * and consistent user-friendly messages.
 * ES: Excepción verificada que representa fallas al leer o parsear JSON.
 * Envolver excepciones de E/S y Jackson en un solo tipo da una API estable
 * y mensajes consistentes para el usuario.
 */
public class JsonReadException extends IOException {
  /** EN/ES: message-only constructor | constructor solo con mensaje */
  public JsonReadException(String message) { super(message); }
  /** EN/ES: message + cause constructor | constructor con mensaje y causa */
  public JsonReadException(String message, Throwable cause) { super(message, cause); }
}
