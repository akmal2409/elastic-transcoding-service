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
      String filename,
      String codec,
      int width,
      int height
  ) {}

  public static record Audio(
      String filename,
      String codec,
      String lang
  ) {
  }

  public static record Subtitles(
      String filename,
      String codec,
      String lang
  ) {}
}
