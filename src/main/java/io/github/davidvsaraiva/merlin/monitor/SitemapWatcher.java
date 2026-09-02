package io.github.davidvsaraiva.merlin.monitor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class SitemapWatcher {

    private static final Logger LOG = LoggerFactory.getLogger(SitemapWatcher.class);

    private static final String WORKSHOP_PATH_PREFIX = "https://www.leroymerlin.pt/bricolage/workshops/";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final String sitemapUrl;
    private final HttpClient httpClient;

    public SitemapWatcher(String sitemapUrl) {
        this.sitemapUrl = sitemapUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
    }

    /**
     * @return known workshop URLs mapped to their title (from the sitemap's own
     * {@code image:title}, falling back to a title derived from the URL slug if absent)
     * and thumbnail image URL (from {@code image:loc}, if present).
     */
    public Map<String, SitemapWorkshop> fetchWorkshops() throws IOException, InterruptedException {
        LOG.info("Fetching sitemap from {}", sitemapUrl);
        HttpRequest request = HttpRequest.newBuilder(URI.create(sitemapUrl))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("Unexpected status code " + response.statusCode() + " fetching sitemap");
        }

        Map<String, SitemapWorkshop> workshops = extractWorkshops(response.body());
        LOG.debug("Found {} workshop entries in sitemap", workshops.size());
        return workshops;
    }

    static Map<String, SitemapWorkshop> extractWorkshops(String sitemapXml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            Document doc = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(sitemapXml.getBytes(StandardCharsets.UTF_8)));

            NodeList urlNodes = doc.getElementsByTagName("url");
            Map<String, SitemapWorkshop> workshops = new LinkedHashMap<>();
            for (int i = 0; i < urlNodes.getLength(); i++) {
                Element urlElement = (Element) urlNodes.item(i);
                String url = firstChildText(urlElement, "loc");
                if (url == null || !isWorkshopUrl(url)) {
                    continue;
                }

                String imageTitle = firstChildText(urlElement, "image:title");
                String title = (imageTitle != null && !imageTitle.isBlank())
                        ? imageTitle.trim()
                        : deriveTitle(url);
                String imageUrl = firstChildText(urlElement, "image:loc");
                workshops.put(url, new SitemapWorkshop(title, imageUrl));
            }
            return workshops;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse sitemap XML", e);
        }
    }

    private static String firstChildText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent().trim();
    }

    private static boolean isWorkshopUrl(String url) {
        // Excludes the bare listing page itself (exactly WORKSHOP_PATH_PREFIX).
        return url.startsWith(WORKSHOP_PATH_PREFIX) && url.length() > WORKSHOP_PATH_PREFIX.length();
    }

    // Fallback only, used when a workshop's sitemap entry has no image:title.
    static String deriveTitle(String url) {
        String slug = url.substring(url.lastIndexOf('/') + 1);
        if (slug.endsWith(".html")) {
            slug = slug.substring(0, slug.length() - ".html".length());
        }

        StringBuilder title = new StringBuilder();
        for (String word : slug.split("-")) {
            if (word.isBlank()) {
                continue;
            }
            if (title.length() > 0) {
                title.append(' ');
            }
            title.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return title.toString();
    }
}