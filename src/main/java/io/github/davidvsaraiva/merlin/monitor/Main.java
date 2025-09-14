package io.github.davidvsaraiva.merlin.monitor;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
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

    private static final String FORM_URL = System.getenv("FORM_TO_MONITOR_URL");
    private static final Path STORE_PATH = Path.of(System.getProperty("user.home"), "workshops.json");

    private static final List<String> STORES = List.of("Loulé", "Albufeira");

    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--help")) {
            printHelp();
            return;
        }

        boolean once = Arrays.asList(args).contains("--once");
        long minutesInterval = parseInterval(args, 360);

        if (once) {
            // just run once and exit
            safeRun("Single run (--once) started", "Single run (--once) finished");
            return ; // exit
        }
        schedulePeriodicRun(minutesInterval);
    }

    private static void schedulePeriodicRun(long minutesInterval) {
        LOG.info("Starting monitor. Stores={}, intervalMinutes={}",STORES, minutesInterval);
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
        var watcher = new FormWatcher(FORM_URL);
        var notifier = EmailNotifier.fromEnv();

        var currentState = repo.loadOrCreate();
        Map<String, WorkshopsRepository.StoreData> currentStatePerStore = currentState.stores();
        List<String> newOnes = new ArrayList<>();

        for(String storeName : STORES) {
            MDC.put("store", "[" + storeName + "]");
            try {
                LOG.info("Fetching workshops from");
                // 1) scrape current list of titles for this store
                List<String> scrapedTitles = watcher.fetchWorkshopsForStore(storeName);
                LOG.debug("Scraped {} items", scrapedTitles.size());

                // 2) ensure a StoreData bucket exists
                var currentStoreData = currentStatePerStore.computeIfAbsent(
                        storeName,
                        s -> new WorkshopsRepository.StoreData(new LinkedHashMap<>(), null)
                );

                // 3) diff: add any new titles
                var byTitle = currentStoreData.getWorkshops();
                for(String title: scrapedTitles) {
                    if(!byTitle.containsKey(title)) {
                        byTitle.put(title, new WorkshopEntry(title)); // firstSeen = Instant.now()
                        newOnes.add("[" + storeName + "] " + title);
                    }
                }
                // 4) update lastChecked for this store
                currentStoreData.setLastChecked(Instant.now().toString());
            } finally {
                MDC.remove("store");
            }

        }
        // 5) Save new state with refreshed lastUpdated
        WorkshopState newState = new WorkshopState(currentStatePerStore, Instant.now().toString());
        repo.save(newState);

        // 6) Notify if anything new
        if(!newOnes.isEmpty()) {
            LOG.info("Detected {} new workshops", newOnes.size());
            String subject = "Novos workshops (" + newOnes.size() + ")";
            String body = String.join("\n", newOnes);
            body = body.concat("\nCheck form here: " + FORM_URL);

            try {
                notifier.send(subject, body);
                LOG.info("Email sent (" + newOnes.size() + " new).");
            } catch (Exception e) {
                LOG.error("Email failed");
            }
        } else {
            LOG.info("No new workshops");
        }

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
            Workshop Monitor - Form Watcher

            Usage:
              java -jar workshop-monitor.jar [options]

            Options:
              --once                   Run a single check and exit (cron mode)
              --interval-minutes <N>   Set interval between checks (default: 360 = 6 hours)
              --help                   Show this help message

            Environment variables (SMTP):
              SMTP_HOST              SMTP server hostname (e.g. smtp.gmail.com)
              SMTP_PORT              SMTP port (default 587)
              SMTP_STARTTLS          true|false (default true)
              SMTP_USERNAME          SMTP username (e.g. your@gmail.com)
              SMTP_PASSWORD          SMTP password or app password
              SMTP_FROM              From email address
              SMTP_TO                To email address
              FORM_TO_MONITOR_URL    Form to monitor URL
              LOG_LEVEL              Log level for the application logs
            
            Environment variable (Selenium):
              HEADLESS_MODE run browser in headless mode (default: true)

            Logging:
              LOG_LEVEL       Root log level (TRACE, DEBUG, INFO, WARN, ERROR). Default: INFO

            Data:
              workshops.json  Stored in your home directory (~). Keeps track of seen workshops
                               and acts as the feed for an Android app or other clients.

            Monitored stores:
              Loulé, Albufeira

            Examples:
              java -jar workshop-monitor.jar --interval-minutes 120
              java -jar workshop-monitor.jar --once
            """);
    }
}