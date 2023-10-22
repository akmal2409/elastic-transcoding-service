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

  public String getS3Region() {
    return "us-east-1";
  }

  public String getS3Endpoint() {
    return "http://localhost:9000";
  }

  public Path getMediaFolder() {
    return Path.of("/tmp");
  }
}
