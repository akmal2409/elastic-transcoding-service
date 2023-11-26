package io.github.akmal2409.ets.orchestrator

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName


fun createPostgres() =
    PostgreSQLContainer(DockerImageName.parse("postgres:alpine3.18"))

fun createLocalStack() =
    LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))

fun configurePostgres(container: PostgreSQLContainer<*>, registry: DynamicPropertyRegistry) {
    registry.add("spring.datasource.url", container::getJdbcUrl)
    registry.add("spring.datasource.username", container::getUsername)
    registry.add("spring.datasource.password", container::getPassword)
}

fun configureLocalstack(container: LocalStackContainer, registry: DynamicPropertyRegistry) {
    registry.add("app.aws.s3-endpoint", container::getEndpoint)
    registry.add("app.aws.region-name", container::getRegion)
    registry.add("aws.credentials.access-key-id", container::getAccessKey)
    registry.add("aws.credentials.secret-access-key", container::getSecretKey)
}
