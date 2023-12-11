package io.github.akmal2409.ets.orchestrator.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class AppConfiguration {

    @Bean
    @Primary
    fun mapper(): ObjectMapper = ObjectMapper()
        .findAndRegisterModules()
        .registerKotlinModule()
}
