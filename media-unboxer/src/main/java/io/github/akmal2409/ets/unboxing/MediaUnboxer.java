package io.github.akmal2409.ets.unboxing;

import static io.github.akmal2409.ets.WorkerConstants.DEFAULT_AUDIO_CONTAINER;
import static io.github.akmal2409.ets.WorkerConstants.DEFAULT_CONTAINER_FORMAT;
import static io.github.akmal2409.ets.WorkerConstants.DEFAULT_SUBTITLES_CONTAINER;
import static io.github.akmal2409.ets.WorkerConstants.DEFAULT_VIDEO_CONTAINER;

import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import com.github.kokorin.jaffree.ffprobe.Stream;
import io.github.akmal2409.ets.exception.FileNotFoundException;
import io.github.akmal2409.ets.unboxing.MediaCollection.Audio;
import io.github.akmal2409.ets.unboxing.MediaCollection.Video;
import io.github.akmal2409.ets.unboxing.MediaStreams.AudioStream;
import io.github.akmal2409.ets.unboxing.MediaStreams.Subtitles;
import io.github.akmal2409.ets.unboxing.MediaStreams.VideoStream;
import io.github.akmal2409.ets.utils.FileUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.jetbrains.annotations.NotNull;

public class MediaUnboxer {

  private static final String LANGUAGE_TAG = "language";

  private final FFmpegExecutor ffmpegExecutor;
  private final FFprobe ffprobe;
  private final ExecutorService executorService;
  private final Duration taskTimeout;

  public MediaUnboxer(FFmpegExecutor ffmpegExecutor, FFprobe fFprobe,
      ExecutorService executorService, Duration taskTimeout) {
    this.ffmpegExecutor = ffmpegExecutor;
    this.ffprobe = fFprobe;
    this.executorService = executorService;
    this.taskTimeout = taskTimeout;
  }

  /**
   * Will convert source media file to another with the same name but different container format at
   * the specified location out.
   *
   * @param src                   media file path
   * @param out                   output directory path!
   * @param outputContainerFormat desired container format
   * @return path to the converted media file
   */
  public Path convertContainerFormat(Path src, Path out, ContainerFormat outputContainerFormat,
      MediaType mediaType) {
    if (!Files.exists(src)) {
      throw new FileNotFoundException(String.format("Provided media file %s doesn't exist", src));
    }

    if (Files.exists(out) && !Files.isDirectory(out)) {
      throw new ConversionException("Output path is not a directory: " + out);
    }

    try {
      Files.createDirectories(out);
    } catch (IOException e) {
      throw new ConversionException("Cannot create output directories", e);
    }

    final var outputFilePath = out.resolve(
        String.format("%s.%s", FileUtils.stripExtension(src.getFileName().toString()),
            outputContainerFormat.getExtensionByType(mediaType)));

    if (outputFilePath.equals(src)) {
      return src; // already in the same format
    }

    final var ffmpegJobBuilder = new FFmpegBuilder()
                                     .addInput(src.toString())
                                     .addOutput(outputFilePath.toString())
                                     .addExtraArgs("-map", "0")
                                     .addExtraArgs("-c", "copy")
                                     .done();

    ffmpegExecutor.createJob(ffmpegJobBuilder).run();
    return null;
  }

  /**
   * Analyses the media file by extracting the video, audio and subtitle information.
   *
   * @param mediaPath path to the media
   */
  public MediaStreams analyseStreams(Path mediaPath) {
    final FFprobeResult result = ffprobe.setShowStreams(true)
                                     .setInput(mediaPath)
                                     .execute();

    final List<AudioStream> audioStreams = new ArrayList<>();
    final List<VideoStream> videoStreams = new ArrayList<>();
    final List<Subtitles> subtitles = new ArrayList<>();

    String codec;

    for (Stream stream : result.getStreams()) {
      codec = stream.getCodecName();

      switch (stream.getCodecType()) {
        case AUDIO -> audioStreams.add(
            new AudioStream(stream.getIndex(), codec, stream.getTag(LANGUAGE_TAG)));
        case SUBTITLE ->
            subtitles.add(new Subtitles(stream.getIndex(), codec, stream.getTag(LANGUAGE_TAG)));
        case VIDEO -> {
          final var videoStream = new VideoStream(stream.getIndex(), codec,
              stream.getHeight(), stream.getWidth(), stream.getAvgFrameRate().doubleValue());
          videoStreams.add(videoStream);
        }
        default -> {/* ignore */}
      }
    }

    return new MediaStreams(videoStreams, audioStreams, subtitles);
  }

  public MediaCollection unboxMediaContainer(@NotNull Path src, @NotNull Path outDir) {
    if (!Files.exists(src)) {
      throw new FileNotFoundException("Source file " + src + " was not found");
    }

    if (Files.exists(outDir) && !Files.isDirectory(outDir)) {
      throw new ConversionException("Output " + outDir + " is not a directory");
    }

    try {
      Files.createDirectories(outDir);
    } catch (IOException e) {
      throw new ConversionException("Cannot create output directories " + outDir);
    }

    final var streams = analyseStreams(src);

    final var videos = new ArrayList<Video>();
    final var audios = new ArrayList<Audio>();
    final var subtitles = new ArrayList<MediaCollection.Subtitles>();
    Future<?> future;
    final var pendingFutures = new ArrayList<Future<?>>();

    for (VideoStream videoStream : streams.videoStreams()) {
      final var outputPath = outDir.resolve(String.format("video-%d.%s",
          videoStream.index(), DEFAULT_VIDEO_CONTAINER));
      videos.add(new Video(outputPath.getFileName().toString(),
          videoStream.codec(), videoStream.width(), videoStream.height()));
      future = executorService.submit(
          () -> extractStream(src, videoStream.index(), outputPath, DEFAULT_CONTAINER_FORMAT));
      pendingFutures.add(future);
    }

    for (AudioStream audioStream : streams.audioStreams()) {
      final var outputPath = outDir.resolve(String.format("audio-%s-%d.%s",
          audioStream.lang(), audioStream.index(), DEFAULT_AUDIO_CONTAINER));
      audios.add(new Audio(outputPath.getFileName().toString(),
          audioStream.codec(), audioStream.lang()));

      future = executorService.submit(
          () -> extractStream(src, audioStream.index(), outputPath, DEFAULT_CONTAINER_FORMAT));
      pendingFutures.add(future);
    }

    for (Subtitles subtitlesStream : streams.subtitles()) {
      final var outputPath = outDir.resolve(String.format("subtitles-%s-%d.%s",
          subtitlesStream.lang(), subtitlesStream.index(), DEFAULT_SUBTITLES_CONTAINER));
      subtitles.add(new MediaCollection.Subtitles(outputPath.getFileName().toString(),
          subtitlesStream.codec(), subtitlesStream.lang()));

      future = executorService.submit(
          () -> extractStream(src, subtitlesStream.index(), outputPath, DEFAULT_CONTAINER_FORMAT));
      pendingFutures.add(future);
    }

    long operationTimeLimit = taskTimeout.toMillis();
    long start = System.currentTimeMillis();

    for (Future<?> pendingFuture : pendingFutures) {
      try {
        pendingFuture.get(operationTimeLimit, TimeUnit.MILLISECONDS);
        operationTimeLimit -= (System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        if (operationTimeLimit <= 0) {
          throw new OperationTimeoutException("Timeout reached");
        }
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        throw new OperationTimeoutException("Unboxing tasks either errored or timed out", e);
      }
    }

    return new MediaCollection(videos, audios, subtitles,
        outDir);
  }

  private void extractStream(Path src, int streamIndex, Path out, String format) {
    final var ffmpegJob = new FFmpegBuilder()
                              .addInput(src.toString())
                              .overrideOutputFiles(true)
                              .addOutput(out.toString())
                              .addExtraArgs("-map", "0:" + streamIndex)
                              .addExtraArgs("-c", "copy")
                              .setFormat(format)
                              .done();
    ffmpegExecutor.createJob(ffmpegJob).run();
  }
}
