package io.github.akmal2409.ets.orchestrator

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@Testcontainers
@ActiveProfiles("integration-test")
class OrchestratorApplicationTests {

	companion object {
		@Container
		val postgres = createPostgres()

		@Container
		val localStack = createLocalStack().withServices(LocalStackContainer.Service.S3)


		@Bean
		@DynamicPropertySource
		@JvmStatic
		fun configure(registry: DynamicPropertyRegistry) {
			configurePostgres(postgres, registry)
			configureLocalstack(localStack, registry)
		}
	}
	@Test
	fun contextLoads() {
	}

}
