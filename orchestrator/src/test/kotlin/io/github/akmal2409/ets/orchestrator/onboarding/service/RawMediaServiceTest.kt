package io.github.akmal2409.ets.orchestrator.onboarding.service

import io.github.akmal2409.ets.orchestrator.onboarding.domain.RawMediaRepository
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.extension.ExtendWith


@ExtendWith(MockKExtension::class)
class RawMediaServiceTest {

    @MockK
    lateinit var repository: RawMediaRepository

    @InjectMockKs
    lateinit var service: RawMediaService


}
