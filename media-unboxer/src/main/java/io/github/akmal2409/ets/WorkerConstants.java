package io.github.akmal2409.ets;

public final class WorkerConstants {

  private WorkerConstants() {
    throw new IllegalStateException("Cannot instantiate a utility class");
  }

  public static final String WORKER_NAME = "media-unboxer";
  public static final String DEFAULT_VIDEO_CONTAINER = "mkv";
  public static final String DEFAULT_AUDIO_CONTAINER = "mka";
  public static final String DEFAULT_SUBTITLES_CONTAINER = "mks";
  public static final String DEFAULT_CONTAINER_FORMAT = "matroska";
}
