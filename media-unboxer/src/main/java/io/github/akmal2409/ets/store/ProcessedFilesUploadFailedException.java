package io.github.akmal2409.ets.store;

import java.nio.file.Path;

public class ProcessedFilesUploadFailedException extends RuntimeException {
  private static final String MSG_FORMAT = "Upload of directory %s to bucket %s with keyPrefix %s failed";
  private final transient Path directory;
  private final String bucket;
  private final String keyPrefix;

  public ProcessedFilesUploadFailedException(Path directory, String bucket, String keyPrefix) {
    super(String.format(MSG_FORMAT, directory, bucket, keyPrefix));
    this.directory = directory;
    this.bucket = bucket;
    this.keyPrefix = keyPrefix;
  }

  public ProcessedFilesUploadFailedException(Throwable cause, Path directory, String bucket, String keyPrefix) {
    super(String.format(MSG_FORMAT, directory, bucket, keyPrefix), cause);
    this.directory = directory;
    this.bucket = bucket;
    this.keyPrefix = keyPrefix;
  }
}
