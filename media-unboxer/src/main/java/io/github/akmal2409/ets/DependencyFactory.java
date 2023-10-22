package io.github.akmal2409.ets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.ConnectionFactory;
import io.github.akmal2409.ets.store.MediaStore;
import java.net.URI;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

/**
 * Configures the dependencies based on the {@link Configuration}
 */
public class DependencyFactory {
  private final Configuration configuration;

  private DependencyFactory(Configuration configuration) {
    this.configuration = configuration;
  }

  public static DependencyFactory withConfiguration(Configuration configuration) {
    return new DependencyFactory(configuration);
  }

  public AwsCredentialsProvider newAwsCredentialsProvider() {
    return DefaultCredentialsProvider.create(); // checks system and env variables
  }

  public S3Client newS3Client(AwsCredentialsProvider credentialsProvider) {
    return S3Client.builder()
               .region(Region.of(configuration.getS3Region()))
               .endpointOverride(URI.create(configuration.getS3Endpoint()))
               .credentialsProvider(credentialsProvider)
               .forcePathStyle(true)
               .build();
  }

    public S3AsyncClient newS3AsyncClient(AwsCredentialsProvider credentialsProvider) {
      return S3AsyncClient.builder()
                 .region(Region.of(configuration.getS3Region()))
                 .endpointOverride(URI.create(configuration.getS3Endpoint()))
                 .credentialsProvider(credentialsProvider)
                 .forcePathStyle(true)
                 .build();
    }

  public S3TransferManager newS3TransferManager(S3AsyncClient asyncClient) {
    return S3TransferManager.builder()
               .s3Client(asyncClient)
               .build();
  }

  public ConnectionFactory newConnectionFactory() {
    final var factory = new ConnectionFactory();
    factory.setHost(configuration.getRabbitMQHost());
    factory.setPort(configuration.getRabbitMQPort());

    return factory;
  }

  public ObjectMapper newObjectMapper() {
    return new ObjectMapper()
               .findAndRegisterModules();
  }

  public MediaStore newS3Store(S3TransferManager transferManager) {
    return new MediaStore(configuration.getMediaFolder(),
        transferManager);
  }
}
