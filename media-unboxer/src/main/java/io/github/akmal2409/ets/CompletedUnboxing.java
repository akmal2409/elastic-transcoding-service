package io.github.akmal2409.ets;

import io.github.akmal2409.ets.unboxing.MediaCollection.Audio;
import io.github.akmal2409.ets.unboxing.MediaCollection.Subtitles;
import io.github.akmal2409.ets.unboxing.MediaCollection.Video;
import java.util.List;
import java.util.UUID;

public record CompletedUnboxing(
    UUID jobId,
    List<Video> videos,
    List<Audio> audio,
    List<Subtitles> subtitles,
    String outputPrefix
) {

}
