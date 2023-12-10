package io.github.akmal2409.ets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import io.github.akmal2409.ets.store.MediaStore;
import io.github.akmal2409.ets.unboxing.ContainerFormat;
import io.github.akmal2409.ets.unboxing.MediaCollection;
import io.github.akmal2409.ets.unboxing.MediaType;
import io.github.akmal2409.ets.unboxing.MediaUnboxer;
import io.github.akmal2409.ets.utils.FileUtils;
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
  private final MediaStore mediaStore;
  private final MediaUnboxer mediaUnboxer;
  private final String outboundQueue;
  private final String outboundExchange;

  public JobConsumer(Channel channel, ObjectMapper objectMapper, MediaStore s3Store,
      MediaUnboxer mediaUnboxer, String outboundQueue, String outboundExchange) {
    super(channel);
    this.objectMapper = objectMapper;
    this.mediaStore = s3Store;
    this.mediaUnboxer = mediaUnboxer;
    this.outboundQueue = outboundQueue;
    this.outboundExchange = outboundExchange;
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

    log.debug("message=Received job;job_id={};source={};output={};worker={}",
        job.jobId(), job.source(), job.out(), WorkerConstants.WORKER_NAME);

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

    final var mediaPath = mediaStore.downloadSource(job.jobId(), job.source());

    try {
      // unify container format for all videos
      final var convertedMediaPath = mediaUnboxer.convertContainerFormat(mediaPath,
          mediaPath.getParent(), ContainerFormat.MKV, MediaType.VIDEO);
      final var unboxedFilesPath = convertedMediaPath.getParent().resolve("unboxed");

      final MediaCollection mediaCollection =
          mediaUnboxer.unboxMediaContainer(convertedMediaPath, unboxedFilesPath);

      // upload unboxed media to the output destination
      mediaStore.uploadProcessedFiles(job.out(), unboxedFilesPath);

      final var report = new CompletedUnboxing(job.jobId(),
          mediaCollection.videos(), mediaCollection.audio(), mediaCollection.subtitles(),
          job.out()
      );

      getChannel().basicPublish(outboundExchange, outboundQueue, null,
          objectMapper.writeValueAsBytes(report));
      getChannel().basicAck(envelope.getDeliveryTag(), false);
    } finally {
      FileUtils.deleteDirectory(mediaPath.getParent());
    }
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
