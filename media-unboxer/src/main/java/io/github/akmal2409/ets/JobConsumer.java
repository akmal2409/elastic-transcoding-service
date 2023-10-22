package io.github.akmal2409.ets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import io.github.akmal2409.ets.store.MediaStore;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.StringUtils;

public class JobConsumer extends DefaultConsumer {

  private static class InvalidManifestException extends RuntimeException {

    public InvalidManifestException(String message) {
      super(message);
    }
  }

  private static final Logger log = LoggerFactory.getLogger(JobConsumer.class);
  private final ObjectMapper objectMapper;
  private final MediaStore s3Store;

  public JobConsumer(Channel channel, ObjectMapper objectMapper, MediaStore s3Store) {
    super(channel);
    this.objectMapper = objectMapper;
    this.s3Store = s3Store;
  }

  @Override
  public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties,
      byte[] body) throws IOException {
    Job job = null;

    try {
      job = objectMapper.readValue(body, Job.class);
    } catch (JsonProcessingException e) {
      log.error(
          "message=Invalid job manifest received;worker={};consumer_tag={};routing_key={};exchange={}",
          WorkerConstants.WORKER_NAME, consumerTag, envelope.getRoutingKey(),
          envelope.getExchange(), e);
      getChannel().basicAck(envelope.getDeliveryTag(), false); // TODO: Send to DLQ
      return;
    }

    log.debug("message=Received job;job_id={};worker={}",
        job.jobId(), WorkerConstants.WORKER_NAME);

    try {
      validateJob(job);
    } catch (InvalidManifestException e) {
      log.error(
          "message={};worker={};job_id={};source={};consumer_tag={};routing_key={};exchange={}",
          e.getMessage(), WorkerConstants.WORKER_NAME, job.jobId(), job.source(), consumerTag,
          envelope.getRoutingKey(), envelope.getExchange(), e);
      getChannel().basicAck(envelope.getDeliveryTag(), false); // TODO: Send to DLQ
      return;
    }

    final var mediaPath = s3Store.downloadSource(job.jobId(), job.source());

    // Step 3: Get all streams
    // Step 4: Extract all streams
    // Step 5: Analyse
    // Step 6: Build report
    // Step 7: Upload
    // Step 8: Finish

    getChannel().basicAck(envelope.getDeliveryTag(), false);
  }

  private void validateJob(Job job) {
    if (job.jobId() == null) {
      throw new InvalidManifestException("jobId is null");
    }

    if (StringUtils.isEmpty(job.source())) {
      throw new InvalidManifestException("source is empty");
    }

    if (!job.source().startsWith("s3://")) {
      throw new InvalidManifestException("source doesn't start with s3://");
    }
  }
}
