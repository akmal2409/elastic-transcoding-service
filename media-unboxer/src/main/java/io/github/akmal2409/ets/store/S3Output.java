package io.github.akmal2409.ets.store;

import java.util.Arrays;

/**
 * Class represents S3 output key i.e. s3://bucket/key which may or may not contain output file name
 */
public class S3Output {

  public static final String PROTOCOL_PREFIX = "s3://";

  private final String bucket;
  private final String key;

  private S3Output(String bucket, String key) {
    this.bucket = bucket;
    this.key = key;
  }

  /**
   * Converts string representation (e.g.g s3://bucket/key) to a typed object.
   *
   * @param output output protocol formatted string
   * @throws InvalidSourceException if the source is malformed.
   */
  public static S3Output from(String output) {
    if (output == null ||
            !output.startsWith(PROTOCOL_PREFIX)
            || output.length() == PROTOCOL_PREFIX.length()) {
      throw new InvalidSourceException(
          "Invalid source supplied. Either its null, empty or doesn't start with s3:// Source: "
              + output);
    }

    final var withoutProtocol = output.substring(PROTOCOL_PREFIX.length());
    final var parts = withoutProtocol.split("/");

    if (parts.length < 1) {
      throw new InvalidSourceException("Invalid output: " + output + " expected at least a bucket name");
    }

    final var bucket = parts[0];
    final var key = String.join("/", Arrays.copyOfRange(parts, 1, parts.length));

    return new S3Output(bucket,
        key);
  }

  public String getBucket() {
    return bucket;
  }

  public String getKey() {
    return key;
  }
}
