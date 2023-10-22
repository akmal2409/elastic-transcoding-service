package io.github.akmal2409.ets.unboxing;

import java.nio.file.Path;
import java.util.List;

public record MediaCollection(
    List<Video> videos,
    List<Audio> audio,
    List<Subtitles> subtitles,
    Path basePath
) {

  public static record Video(
      String fileName,
      String codec,
      int width,
      int height
  ) {}

  public static record Audio(
      String fileName,
      String codec,
      String lang
  ) {
  }

  public static record Subtitles(
      String fileName,
      String codec,
      String lang
  ) {}
}
