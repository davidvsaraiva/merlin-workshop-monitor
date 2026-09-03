package io.github.davidvsaraiva.merlin.monitor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.github.davidvsaraiva.merlin.monitor.Config.getEnvOrDefault;

/**
 * Optional "dead man's switch" heartbeat: pings an external monitoring service
 * (e.g. healthchecks.io) after a successful run, so a silent Pi/app outage gets
 * flagged by that service's own grace-period alerting instead of going unnoticed.
 */
public class HealthcheckNotifier {

    private static final Logger LOG = LoggerFactory.getLogger(HealthcheckNotifier.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final String pingUrl;
    private final HttpClient httpClient;

    public HealthcheckNotifier(String pingUrl) {
        this.pingUrl = pingUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
    }

    public static HealthcheckNotifier fromEnv() {
        return new HealthcheckNotifier(getEnvOrDefault("HEALTHCHECK_URL", null));
    }

    public void pingSuccess() {
        if (pingUrl == null || pingUrl.isBlank()) {
            LOG.debug("No HEALTHCHECK_URL configured, skipping heartbeat ping");
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(pingUrl))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            LOG.debug("Heartbeat ping sent, status {}", response.statusCode());
        } catch (Exception e) {
            // A flaky heartbeat ping must never affect the run's own outcome.
            LOG.warn("Failed to send heartbeat ping", e);
        }
    }
}
