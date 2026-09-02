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
        assertEquals(workshopState.workshops().size(), 1);

        String knownUrl = "https://www.leroymerlin.pt/bricolage/workshops/workshop-como-tratar-de-um-bonsai.html";
        assertEquals(workshopState.workshops().get(knownUrl).title(), "Workshop Como Tratar De Um Bonsai");
    }
}