package io.github.davidvsaraiva.merlin.monitor;

import java.util.Map;

public record WorkshopState(
        Map<String, WorkshopsRepository.StoreData> stores,
        String lastUpdated) {
}