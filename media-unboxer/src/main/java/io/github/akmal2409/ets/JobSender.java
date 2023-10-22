package io.github.akmal2409.ets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import org.slf4j.LoggerFactory;

public class JobSender {

  public static void main(String[] args) throws IOException, TimeoutException {
    Configuration configuration = new Configuration();

    final var connectionFactory = new ConnectionFactory();
    connectionFactory.setHost(configuration.getRabbitMQHost());
    connectionFactory.setPort(configuration.getRabbitMQPort());

    final var connection = connectionFactory.newConnection();
    final var channel = connection.createChannel();
    final var jobId = UUID.fromString("3104B65B-589A-4190-B366-3BCE6570533E");

    channel.basicPublish("", configuration.getInboundTaskQueueName(),
        null, new ObjectMapper().writeValueAsBytes(
            new Job(jobId, "s3://raw/complete.mkv", "s3://unboxed")
        ));

    LoggerFactory.getLogger(JobSender.class).info("Complete sending");
  }
}
