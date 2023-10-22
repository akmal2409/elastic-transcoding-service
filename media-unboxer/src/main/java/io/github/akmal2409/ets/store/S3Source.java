package io.github.akmal2409.ets.store;

import java.util.Arrays;

/**
 * Class represents S3 source key i.e. s3://bucket/key
 */
public class S3Source {

  public static final String PROTOCOL_PREFIX = "s3://";

  private final String bucket;
  private final String key;
  private final String fileName;

  private S3Source(String bucket, String key, String fileName) {
    this.bucket = bucket;
    this.key = key;
    this.fileName = fileName;
  }

  /**
   * Converts string representation (e.g.g s3://bucket/key) to a typed object.
   *
   * @param source source string
   * @throws InvalidSourceException if the source is malformed.
   */
  public static S3Source from(String source) {
    if (source == null ||
            !source.startsWith(PROTOCOL_PREFIX)
            || source.length() == PROTOCOL_PREFIX.length()) {
      throw new InvalidSourceException(
          "Invalid source supplied. Either its null, empty or doesn't start with s3:// Source: "
              + source);
    }

    final var withoutProtocol = source.substring(PROTOCOL_PREFIX.length());
    final var parts = withoutProtocol.split("/");

    if (parts.length < 2) {
      throw new InvalidSourceException("Invalid source: " + source + " expected bucket and a key");
    }

    final var bucket = parts[0];
    final var fileName = parts[parts.length - 1];
    final var key = String.join("/", Arrays.copyOfRange(parts, 1, parts.length));

    return new S3Source(bucket,
        key, fileName);
  }

  public String getBucket() {
    return bucket;
  }

  public String getKey() {
    return key;
  }

  public String getFileName() {
    return fileName;
  }
}
