package io.github.akmal2409.ets;

import java.util.UUID;

public record Job(
    UUID jobId,
    String source,
    String out
) {

}
