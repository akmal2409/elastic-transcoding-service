package io.github.akmal2409.ets.unboxing;

public enum ContainerFormat {
  MKV("mkv", "mka", "mks");

  final String videoExtension;
  final String audioExtension;
  final String subtitlesExtension;

  ContainerFormat(String videoExtension, String audioExtension, String subtitlesExtension) {
    this.videoExtension = videoExtension;
    this.audioExtension = audioExtension;
    this.subtitlesExtension = subtitlesExtension;
  }

  public String getExtensionByType(MediaType type) {
    return switch (type) {
      case AUDIO -> this.audioExtension;
      case SUBTITLES -> this.subtitlesExtension;
      default -> this.videoExtension;
    };
  }
}
