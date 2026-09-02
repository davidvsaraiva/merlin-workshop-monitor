package io.github.davidvsaraiva.merlin.monitor;

import java.util.Map;

public record WorkshopState(
        Map<String, KnownWorkshop> workshops,
        String lastUpdated) {
}
