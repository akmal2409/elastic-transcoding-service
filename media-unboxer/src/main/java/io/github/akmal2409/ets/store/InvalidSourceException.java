package io.github.akmal2409.ets.store;

/**
 * Exception that is thrown when invalid file source provided.
 * All file sources are provided as plain strings and then validated.
 */
public class InvalidSourceException extends RuntimeException {

  public InvalidSourceException(String message) {
    super(message);
  }

  public InvalidSourceException(String message, Throwable cause) {
    super(message, cause);
  }
}
