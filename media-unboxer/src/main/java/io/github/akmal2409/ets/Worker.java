package io.github.akmal2409.ets;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Worker {

  private static final Logger log = LoggerFactory.getLogger(Worker.class);

  public static void main(String[] args) {
    final var config = new Configuration();
    final var dependencyFactory = DependencyFactory.withConfiguration(config);

    final var connectionFactory = dependencyFactory.newConnectionFactory();
    final var objectMapper = dependencyFactory.newObjectMapper();
    final var mediaStore = dependencyFactory.newS3Store(
        dependencyFactory.newS3TransferManager(dependencyFactory.newS3AsyncClient(
            dependencyFactory.newAwsCredentialsProvider()
        )));
    final var mediaUnboxer = dependencyFactory.newMediaUnboxer(
        dependencyFactory.newFFmpegExecutor(), dependencyFactory.newFFprobe(),
        Executors.newVirtualThreadPerTaskExecutor()
    );

    try {
      final var connection = connectionFactory.newConnection();
      final var channel = connection.createChannel();

      channel.basicQos(config.getParallelism());
      channel.queueDeclare(config.getInboundTaskQueueName(), true, false, false, null);
      channel.queueDeclare(config.getOutboundTaskQueueName(), true, false, false, null);

      channel.basicConsume(config.getInboundTaskQueueName(),
          new JobConsumer(channel, objectMapper, mediaStore,
              mediaUnboxer, config.getOutboundTaskQueueName(), ""));
      log.debug(
          "message=Started media-unboxer worker. Listening to queue {} at host rabbitmq://{}:{}",
          config.getInboundTaskQueueName(), config.getRabbitMQHost(), config.getRabbitMQPort());
    } catch (TimeoutException | IOException e) {
      log.error("message=Error while establishing connection to rabbitmq host;"
                    + "rabbit_host={}", String.format("%s:%d", config.getRabbitMQHost(),
          config.getRabbitMQPort()), e);
    }
  }
}
