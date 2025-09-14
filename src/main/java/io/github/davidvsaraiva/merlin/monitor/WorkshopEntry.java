package io.github.davidvsaraiva.merlin.monitor;

import java.time.Instant;

public record WorkshopEntry(String title, String firstSeen) {

    public WorkshopEntry(String title) {
        this(title, Instant.now().toString());
    }
}