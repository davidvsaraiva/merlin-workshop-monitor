package io.github.davidvsaraiva.merlin.monitor;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URL;
import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class FormWatcherTest {

    static String fileUrl;

    @BeforeMethod
    public void resolveLocalFile() throws Exception {
        URL res = FormWatcherTest.class.getResource("/io/github/davidvsaraiva/merlin/monitor/example.html");
        assertNotNull(res, "example.html must be on the test classpath");
        URI uri = res.toURI();
        fileUrl = uri.toString();
    }

    @Test
    public void readsWorkshopsFromLocalFile() {
        // Pass the file:// URL to your class
        FormWatcher watcher = new FormWatcher(fileUrl);

        List<String> workshops = watcher.fetchWorkshopsForStore("Loulé");

        assertFalse(workshops.isEmpty(), "Should list workshops");
        assertTrue(workshops.stream().anyMatch(s -> s.contains("Como regularizar e colar cerâmica em parede e pavimento")), "Expected a known workshop");
    }
}
