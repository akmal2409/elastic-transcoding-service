package io.github.akmal2409.ets.orchestrator

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AppConfiguration {

    @Bean
    fun mapper(): ObjectMapper = ObjectMapper().findAndRegisterModules()
}
