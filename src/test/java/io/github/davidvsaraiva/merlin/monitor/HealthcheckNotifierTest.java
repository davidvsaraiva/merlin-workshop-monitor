package io.github.davidvsaraiva.merlin.monitor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class HealthcheckNotifierTest {

    @Test
    public void pingSuccessIsANoOpWhenUrlNotConfigured() {
        HealthcheckNotifier notifier = new HealthcheckNotifier(null);

        // Should not throw despite there being nothing to ping.
        notifier.pingSuccess();
    }

    @Test
    public void pingSuccessIsANoOpWhenUrlIsBlank() {
        HealthcheckNotifier notifier = new HealthcheckNotifier("   ");

        notifier.pingSuccess();
    }

    @Test
    public void pingSuccessHitsTheConfiguredUrl() throws IOException, InterruptedException {
        AtomicInteger hitCount = new AtomicInteger(0);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ping", exchange -> {
            hitCount.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/ping";
            HealthcheckNotifier notifier = new HealthcheckNotifier(url);

            notifier.pingSuccess();

            assertEquals(hitCount.get(), 1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void pingSuccessDoesNotThrowWhenEndpointIsUnreachable() {
        // Nothing listens on this port - the ping should fail silently, not propagate.
        HealthcheckNotifier notifier = new HealthcheckNotifier("http://127.0.0.1:1/ping");

        notifier.pingSuccess();
    }
}
