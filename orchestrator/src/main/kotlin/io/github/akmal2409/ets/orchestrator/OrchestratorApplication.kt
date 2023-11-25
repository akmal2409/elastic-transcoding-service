package io.github.akmal2409.ets.orchestrator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import java.time.Clock

@SpringBootApplication
class OrchestratorApplication {

	@Bean
	fun clock(): Clock = Clock.systemUTC()
}

fun main(args: Array<String>) {
	runApplication<OrchestratorApplication>(*args)
}
