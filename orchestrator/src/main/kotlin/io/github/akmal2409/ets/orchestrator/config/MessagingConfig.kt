package io.github.akmal2409.ets.orchestrator.config

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

data class RabbitQueues(
    val rawFileUploaded: String,
    val rawFileUploadedRetry: String,
    val rawFileUploadedDlq: String,
    val mediaUnboxingQueue: String,
    val mediaUnboxingJobCompletionQueue: String
)

@ConfigurationProperties(prefix = "app.messaging")
data class MessagingProperties(
    val rabbitQueues: RabbitQueues
)

@Configuration
@EnableConfigurationProperties(MessagingProperties::class)
class MessagingConfig {

    @Bean
    fun jsonMessageConverter() = Jackson2JsonMessageConverter()
}
