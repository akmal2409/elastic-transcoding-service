package io.github.akmal2409.ets.orchestrator

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class MockClock(val instant: Instant, val mockZone: ZoneId = ZoneId.of("UTC")): Clock() {
    override fun instant() = instant

    override fun withZone(zone: ZoneId?) = this

    override fun getZone()  = mockZone
}
