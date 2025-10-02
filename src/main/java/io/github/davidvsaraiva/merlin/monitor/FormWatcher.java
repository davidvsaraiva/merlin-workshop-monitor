package io.github.davidvsaraiva.merlin.monitor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.github.davidvsaraiva.merlin.monitor.Config.getEnvOrDefault;

public class FormWatcher {

    private static final Logger LOG = LoggerFactory.getLogger(FormWatcher.class);

    private static final boolean IS_CHROMIUM = Boolean.parseBoolean(getEnvOrDefault("IS_CHROMIUM", "false"));
    private static final String CHROMIUM_BROWSER_PATH = getEnvOrDefault("CHROMIUM_BROWSER_PATH", null);
    private static final String CHROMIUM_DRIVER_PATH = getEnvOrDefault("CHROMIUM_DRIVER_PATH", null);
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    private final String formUrl;

    public FormWatcher(String formUrl) {
        this.formUrl = formUrl;
    }

    public List<String> fetchWorkshopsForStore(String storeName) {
        WebDriver driver = createWebDriver();
        WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);
        try {
            driver.get(formUrl);

            WebElement dropdown = locateDropdown(wait);
            selectStore(storeName, dropdown);
            List<WebElement> labelNodes = collectAllWorkshops(wait, driver);
            if (labelNodes.isEmpty()) {
                LOG.debug("Not able to to read workshops list, attempting to read workshops from XPath...");
                labelNodes = findElementsByXpath(driver);
            }
            LOG.debug("Found {} workshops for store {}", labelNodes.size(), storeName);
            return labelNodes.stream()
                    .map(WebElement::getText)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (TimeoutException te) {
            throw new RuntimeException("Timed out locating store dropdown or workshops list", te);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch workshops for store: " + storeName, e);
        } finally {
            driver.quit();
        }
    }

    private static WebDriver createWebDriver() {
        WebDriver driver;
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--no-sandbox", "--disable-gpu");
        if (Boolean.parseBoolean(getEnvOrDefault("HEADLESS_MODE", "true"))) {
            opts.addArguments("--headless=new");
        }
        if (IS_CHROMIUM) {
            if(CHROMIUM_BROWSER_PATH != null && CHROMIUM_DRIVER_PATH != null) {
                opts.setBinary(CHROMIUM_BROWSER_PATH);
                Path profile = mkTempProfile();
                opts.addArguments("--user-data-dir=" + profile.toAbsolutePath(), "--disable-dev-shm-usage", "--remote-debugging-port=0", "--no-default-browser-check");
                Runtime.getRuntime().addShutdownHook(new Thread(() -> rmDir(profile)));
                ChromeDriverService service = new ChromeDriverService.Builder()
                        .usingDriverExecutable(new File(CHROMIUM_DRIVER_PATH))
                        .withLogFile(new File("/tmp/chromedriver.log"))
                        .build();
                driver = new ChromeDriver(service, opts);
            } else {
                throw new IllegalArgumentException("CHROMIUM_PATH is not set and trying to use chromium");
            }
        }  else {
            driver = new ChromeDriver(opts);
        }
        return driver;
    }


    private static Path mkTempProfile() {
        try {
            return Files.createTempDirectory("selenium-chrome-profile-");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void rmDir(Path dir) {
        try {
            Files.walk(dir).sorted(Comparator.reverseOrder())
                    .forEach(p -> { try { Files.delete(p); } catch (Exception ignored) {} });
        } catch (Exception ignored) {}
    }

    private static List<WebElement> findElementsByXpath(WebDriver driver) {
        List<WebElement> labelNodes;
        By WORKSHOP_LABELS = By.xpath(
                "//ul[contains(@class,'ChoiceStructure')]" +
                "[not(ancestor-or-self::*[@aria-hidden='true' or contains(@style,'display: none') or contains(@style,'visibility: hidden')])]" +
                "//li[contains(@class,'Selection')]//span[contains(@class,'LabelWrapper')]/label");
        labelNodes = driver.findElements(WORKSHOP_LABELS);
        return labelNodes;
    }

    private static List<WebElement> collectAllWorkshops(WebDriverWait wait, WebDriver driver) {
        LOG.debug("Waiting for workshops list...");
        // Wait for the workshops list (checkboxes) to render after selection
        By WORKSHOP_LABELS_CSS = By.cssSelector("ul.ChoiceStructure li.Selection span.LabelWrapper > label");

        // Wait until at least one matching label is actually displayed
        wait.until(d -> d.findElements(WORKSHOP_LABELS_CSS)
                .stream().anyMatch(WebElement::isDisplayed));


        LOG.info("Read workshops list");
        // Collect visible labels paired with those checkboxes.
        // Now collect only the visible ones
        return driver.findElements(WORKSHOP_LABELS_CSS)
                .stream().filter(WebElement::isDisplayed).toList();
    }

    private static void selectStore(String storeName, WebElement dropdown) {
        Select select = new Select(dropdown);
        try {
            LOG.info("Selecting store: {}", storeName);
            select.selectByVisibleText(storeName);
            LOG.debug("Store selected");
        } catch (NoSuchElementException e) {
            LOG.debug("Store not found: {}, using fallback without accents", storeName);
            // Fallback: try without accents if needed (e.g., 'Loule')
            String ascii = storeName.replace("é", "e").replace("É", "E");
            select.selectByVisibleText(ascii);
            LOG.debug("Store selected using fallback");
        }
    }

    private static WebElement locateDropdown(WebDriverWait wait) {
        WebElement dropdown;
        try {
            LOG.info("Finding dropdown...");
            dropdown = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("QR~QID18")));
            dropdown.click();
            LOG.debug("Dropdown found by id");
        } catch (TimeoutException e) {
            LOG.debug("Dropdown not found by id, trying by text");
            // Fallback: by label text if the id ever changes
            dropdown = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//label[contains(normalize-space(.), 'Selecione a sua loja')]/following::select[1]")
            ));
            dropdown.click();
            LOG.debug("Dropdown found by text");
        }
        return dropdown;
    }
}