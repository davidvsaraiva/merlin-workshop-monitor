package io.github.davidvsaraiva.merlin.monitor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class SitemapWatcherTest {

    @Test
    public void extractsOnlyWorkshopUrlsExcludingListingPage() throws IOException, URISyntaxException {
        Map<String, SitemapWorkshop> workshops = SitemapWatcher.extractWorkshops(readSampleSitemap());

        assertEquals(workshops.size(), 2);
    }

    @Test
    public void usesSitemapImageTitleAndImageUrlWhenPresent() throws IOException, URISyntaxException {
        Map<String, SitemapWorkshop> workshops = SitemapWatcher.extractWorkshops(readSampleSitemap());

        SitemapWorkshop info = workshops.get(
                "https://www.leroymerlin.pt/bricolage/workshops/workshop-como-tratar-de-um-bonsai.html");
        assertEquals(info.title(), "WORKSHOP: Como tratar de um bonsai");
        assertEquals(info.imageUrl(), "https://media.adeo.com/media/1111111/media.jpeg");
    }

    @Test
    public void fallsBackToDerivedTitleAndNullImageWhenImageMissing() throws IOException, URISyntaxException {
        Map<String, SitemapWorkshop> workshops = SitemapWatcher.extractWorkshops(readSampleSitemap());

        SitemapWorkshop info = workshops.get(
                "https://www.leroymerlin.pt/bricolage/workshops/workshop-como-renovar-a-iluminacao-com-lexman.html");
        assertEquals(info.title(), "Workshop Como Renovar A Iluminacao Com Lexman");
        assertNull(info.imageUrl());
    }

    @Test
    public void derivesHumanReadableTitleFromSlug() {
        String title = SitemapWatcher.deriveTitle(
                "https://www.leroymerlin.pt/bricolage/workshops/workshop-como-renovar-a-iluminacao-com-lexman.html");

        assertEquals(title, "Workshop Como Renovar A Iluminacao Com Lexman");
    }

    private static String readSampleSitemap() throws IOException, URISyntaxException {
        URL resource = SitemapWatcherTest.class.getResource("/io/github/davidvsaraiva/merlin/monitor/sitemap-sample.xml");
        return Files.readString(Path.of(resource.toURI()));
    }
}