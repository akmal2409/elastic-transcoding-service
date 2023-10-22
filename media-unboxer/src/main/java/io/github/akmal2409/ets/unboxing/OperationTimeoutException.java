package io.github.akmal2409.ets.unboxing;

public class OperationTimeoutException extends RuntimeException {

  public OperationTimeoutException(String message) {
    super(message);
  }

  public OperationTimeoutException(String message, Throwable cause) {
    super(message, cause);
  }
}
