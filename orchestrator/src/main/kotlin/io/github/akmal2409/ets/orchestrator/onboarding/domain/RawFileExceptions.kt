package io.github.akmal2409.ets.orchestrator.onboarding.domain

sealed class RawFileException(val key: RawMediaKey, message: String, cause: Throwable? = null) :
    RuntimeException("RawFileKey=$key. Exception: $message", cause)

sealed class RecoverableRawFileException(
    key: RawMediaKey,
    message: String,
    cause: Throwable? = null
) :
    RawFileException(key, message, cause)

sealed class NonRecoverableRawFileException(
    key: RawMediaKey,
    message: String,
    cause: Throwable? = null
) :
    RawFileException(key, message, cause)

class AlreadyUnboxedRawFileException(key: RawMediaKey) :
    NonRecoverableRawFileException(key, "Raw file already unboxed")

class InvalidRawFileException(key: RawMediaKey, reason: String, cause: Throwable? = null) :
    NonRecoverableRawFileException(key, reason, cause)

class RawFileOnboardingException(key: RawMediaKey, reason: String, cause: Throwable? = null) :
    RecoverableRawFileException(key, reason, cause)

class UnboxingJobFailedStartException(key: RawMediaKey, reason: String, cause: Throwable? = null) :
    RecoverableRawFileException(key, reason, cause)

class NonRecoverableJobStartException(key: RawMediaKey, reason: String, cause: Throwable? = null) :
    NonRecoverableRawFileException(key, reason, cause)

class UnboxingJobNotFoundException(message: String) : RuntimeException(message)


