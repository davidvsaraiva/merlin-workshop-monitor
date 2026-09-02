package io.github.davidvsaraiva.merlin.monitor;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static final String SITEMAP_URL = "https://www.leroymerlin.pt/sitemap-idee-projet1.xml";
    private static final Path STORE_PATH = Path.of(System.getProperty("user.home"), "workshops.json");

    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--help")) {
            printHelp();
            return;
        }

        boolean once = Arrays.asList(args).contains("--once");

        if (once) {
            // just run once and exit
            safeRun("Single run (--once) started", "Single run (--once) finished");
            return ; // exit
        }
        long minutesInterval = parseInterval(args, 60);
        schedulePeriodicRun(minutesInterval);
    }

    private static void schedulePeriodicRun(long minutesInterval) {
        LOG.info("Starting monitor. intervalMinutes={}", minutesInterval);
        // otherwise, schedule periodically
        var exec = Executors.newSingleThreadScheduledExecutor();
        addShutdownHookForScheduler(exec);
        Runnable task = () -> safeRun("Scheduled run start",  "Scheduled run finished");
        exec.scheduleWithFixedDelay(task, 0, minutesInterval, TimeUnit.MINUTES);
    }

    private static void addShutdownHookForScheduler(ScheduledExecutorService exec) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Shutdown signal received; stopping scheduler...");
            exec.shutdown();
            try {
                if (!exec.awaitTermination(10, TimeUnit.SECONDS)) {
                    LOG.warn("Scheduler didn’t stop in time; forcing shutdownNow()");
                    exec.shutdownNow();
                    if (!exec.awaitTermination(5, TimeUnit.SECONDS)) {
                        LOG.warn("Scheduler still not terminated after shutdownNow()");
                    }
                }
            } catch (InterruptedException ie) {
                LOG.warn("Shutdown interrupted; forcing shutdownNow()");
                exec.shutdownNow();
                Thread.currentThread().interrupt();
            }
            LOG.info("Scheduler stopped.");
        }));
    }

    private static void safeRun(String runStartMessage, String runFinishMessage) {
        String runId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("runId", runId);
        try {
            LOG.info(runStartMessage);
            runOnce();
            LOG.info(runFinishMessage);
        } catch (Exception e) {
            LOG.error("Run failed", e);
        } finally {
            MDC.clear();
        }
    }

    private static void runOnce() throws Exception {
        var repo = new WorkshopsRepository(STORE_PATH);
        var watcher = new SitemapWatcher(SITEMAP_URL);
        var notifier = EmailNotifier.fromEnv();

        // 1) load previously known workshops
        var currentState = repo.loadOrCreate();
        boolean isFirstRun = currentState.lastUpdated() == null;
        Map<String, KnownWorkshop> knownWorkshops = currentState.workshops();

        // 2) fetch the current workshop sitemap
        LOG.info("Fetching workshop sitemap from {}", SITEMAP_URL);
        Map<String, SitemapWorkshop> scrapedWorkshops = watcher.fetchWorkshops();
        LOG.debug("Found {} workshop entries", scrapedWorkshops.size());

        // 3) diff: add any new workshops
        List<KnownWorkshop> newOnes = new ArrayList<>();
        for (Map.Entry<String, SitemapWorkshop> scraped : scrapedWorkshops.entrySet()) {
            String url = scraped.getKey();
            if (!knownWorkshops.containsKey(url)) {
                SitemapWorkshop info = scraped.getValue();
                KnownWorkshop entry = new KnownWorkshop(url, info.title(), info.imageUrl());
                knownWorkshops.put(url, entry);
                newOnes.add(entry);
            }
        }

        // 4) save new state with refreshed lastUpdated
        WorkshopState newState = new WorkshopState(knownWorkshops, Instant.now().toString());
        repo.save(newState);

        // 5) notify if anything new (skip on first run, see isFirstRun above)
        if (isFirstRun) {
            // Seed state from whatever currently exists rather than emailing every
            // pre-existing workshop as "new" on first deployment.
            LOG.info("First run - seeded state with {} known workshops, no notification sent",
                    knownWorkshops.size());
        } else if (!newOnes.isEmpty()) {
            LOG.info("Detected {} new workshops", newOnes.size());
            String subject = "Novos workshops (" + newOnes.size() + ")";
            String body = buildEmailBody(newOnes);

            try {
                notifier.send(subject, body);
                LOG.info("{} new workshops found.", newOnes.size());
            } catch (Exception e) {
                LOG.error("Email failed", e);
            }
        } else {
            LOG.info("No new workshops");
        }
    }

    private static String buildEmailBody(List<KnownWorkshop> newOnes) {
        StringBuilder body = new StringBuilder("<html><body>");
        for (KnownWorkshop entry : newOnes) {
            body.append("<p><strong>").append(escapeHtml(entry.title())).append("</strong><br>")
                    .append("<a href=\"").append(entry.url()).append("\">").append(entry.url()).append("</a>");
            if (entry.imageUrl() != null) {
                body.append("<br><img src=\"").append(entry.imageUrl()).append("\" width=\"300\">");
            }
            body.append("</p>");
        }
        return body.append("</body></html>").toString();
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static long parseInterval(String[] args, long defaultMinutes) {
        for(int i = 0; i < args.length - 1; i++) {
            if("--interval-minutes".equals(args[i])) {
                return Long.parseLong(args[i + 1]);
            }
        }
        return defaultMinutes;
    }

    private static void printHelp() {
        System.out.println("""
            Workshop Monitor - Sitemap Watcher

            Usage:
              java -jar workshop-monitor.jar [options]

            Options:
              --once                   Run a single check and exit (cron mode)
              --interval-minutes <N>   Set interval between checks (default: 60 = 1 hour)
              --help                   Show this help message

            Environment variables (SMTP):
              SMTP_HOST              SMTP server hostname (e.g. smtp.gmail.com)
              SMTP_PORT              SMTP port (default 587)
              SMTP_STARTTLS          true|false (default true)
              SMTP_USERNAME          SMTP username (e.g. your@gmail.com)
              SMTP_PASSWORD          SMTP password or app password
              SMTP_FROM              From email address
              SMTP_TO                To email address

            Logging:
              LOG_LEVEL       Root log level (TRACE, DEBUG, INFO, WARN, ERROR). Default: INFO

            Data:
              workshops.json  Stored in your home directory (~). Keeps track of seen workshop URLs.

            Source:
              Leroy Merlin's public workshops sitemap (sitemap-idee-projet1.xml). Reports any
              newly-added workshop page, regardless of store.

            Examples:
              java -jar workshop-monitor.jar --interval-minutes 120
              java -jar workshop-monitor.jar --once
            """);
    }
}
