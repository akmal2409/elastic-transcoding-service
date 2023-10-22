package io.github.akmal2409.ets.unboxing;

import java.util.List;

public record MediaStreams(
    List<VideoStream> videoStreams,
    List<AudioStream> audioStreams,
    List<Subtitles> subtitles
) {

  public static record VideoStream(
      int index,
      String codec,
      int height,
      int width,
      double fps
  ) {

  }

  public static record AudioStream(
      int index,
      String codec,
      String lang // 3 letter ISO code
  ) {

  }

  public static record Subtitles(
      int index,
      String codec,
      String lang
  ) {

  }
}
