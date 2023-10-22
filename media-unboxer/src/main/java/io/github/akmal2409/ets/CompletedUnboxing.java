package io.github.akmal2409.ets;

import io.github.akmal2409.ets.unboxing.MediaCollection.Audio;
import io.github.akmal2409.ets.unboxing.MediaCollection.Subtitles;
import io.github.akmal2409.ets.unboxing.MediaCollection.Video;
import java.util.List;

public record CompletedUnboxing(
    List<Video> videos,
    List<Audio> audio,
    List<Subtitles> subtitles,
    String outputPrefix
) {

}
