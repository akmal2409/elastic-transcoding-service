package io.github.akmal2409.ets;

import java.nio.file.Path;

public class Configuration {

  public String getRabbitMQHost() {
    return "localhost";
  }

  public int getRabbitMQPort() {
    return 5672;
  }

  public int getParallelism() {
    return 1;
  }

  public String getInboundTaskQueueName() {
    return "media-unboxing-job-queue";
  }

  public String getOutboundTaskQueueName() {
    return "media-unboxing-job-completion-queue";
  }

  public String getS3Region() {
    return "us-east-1";
  }

  public String getS3Endpoint() {
    return "http://localhost:9000";
  }

  public Path getMediaFolder() {
    return Path.of("/tmp");
  }

  public Path getFFmpegPath() {
    return Path.of("/opt/homebrew/bin/ffmpeg");
  }

  public Path getFFProbePath() {
    return Path.of("/opt/homebrew/bin/ffprobe");
  }
}
