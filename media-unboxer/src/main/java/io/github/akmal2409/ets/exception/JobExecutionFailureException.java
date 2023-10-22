package io.github.akmal2409.ets.exception;

import java.util.UUID;

public class JobExecutionFailureException extends RuntimeException {

  private final UUID jobId;

  public JobExecutionFailureException(String message, UUID jobId) {
    super(message);
    this.jobId = jobId;
  }

  public JobExecutionFailureException(String message, Throwable cause, UUID jobId) {
    super(message, cause);
    this.jobId = jobId;
  }

  public UUID getJobId() {
    return jobId;
  }
}
