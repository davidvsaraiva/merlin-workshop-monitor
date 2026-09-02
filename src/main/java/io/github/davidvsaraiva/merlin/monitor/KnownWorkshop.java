package io.github.davidvsaraiva.merlin.monitor;

import java.time.Instant;

public record KnownWorkshop(String url, String title, String imageUrl, String firstSeen) {

    public KnownWorkshop(String url, String title, String imageUrl) {
        this(url, title, imageUrl, Instant.now().toString());
    }
}
