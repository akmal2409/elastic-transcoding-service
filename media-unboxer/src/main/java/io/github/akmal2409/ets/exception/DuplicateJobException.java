package io.github.akmal2409.ets.exception;

import java.util.UUID;

public class DuplicateJobException extends JobExecutionFailureException {

  public DuplicateJobException(String message, UUID jobId) {
    super(message, jobId);
  }
}
