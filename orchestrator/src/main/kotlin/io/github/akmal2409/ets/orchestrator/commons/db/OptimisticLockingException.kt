package io.github.akmal2409.ets.orchestrator.commons.db

import java.lang.RuntimeException

data class OptimisticLockingException(
    val entity: String,
    val key: Any,
    val previousVersion: Long,
    val nextVersion: Long
) : RuntimeException("Failed to update $entity with key $key with previous version $previousVersion and next version $nextVersion") {
}
