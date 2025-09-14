package io.github.davidvsaraiva.merlin.monitor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class WorkshopsRepositoryTest {

    @Test
    public void testRepository() throws IOException, URISyntaxException {
        // first, we need to load the file from the classpath
        URL url = WorkshopsRepositoryTest.class.getResource("/io/github/davidvsaraiva/merlin/monitor/workshops.json");
        Path path = Path.of(url.toURI());
        WorkshopsRepository repository = new WorkshopsRepository(path);
        WorkshopState workshopState = repository.loadOrCreate();
        assertEquals(workshopState.stores().size(), 2);
        assertEquals(workshopState.stores().get("Loulé").getWorkshops().size(), 1);
    }
}