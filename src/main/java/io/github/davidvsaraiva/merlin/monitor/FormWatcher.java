package io.github.davidvsaraiva.merlin.monitor;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FormWatcher {

    private static final Logger LOG = LoggerFactory.getLogger(FormWatcher.class);

    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    private final String formUrl;

    public FormWatcher(String formUrl) {
        this.formUrl = formUrl;
        WebDriverManager.chromedriver().setup();
    }

    public List<String> fetchWorkshopsForStore(String storeName) {
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--no-sandbox", "--disable-gpu");
        if (Boolean.parseBoolean(EmailNotifier.getEnv("HEADLESS_MODE", "true"))) {
            opts.addArguments("--headless=new");
        }
        WebDriver driver = new ChromeDriver(opts);
        WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);
        try {
            driver.get(formUrl);

            WebElement dropdown = locateDropdown(wait);
            selectStore(storeName, dropdown);
            List<WebElement> labelNodes = collectAllWorkshops(wait, driver);
            if (labelNodes.isEmpty()) {
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

    private static List<WebElement> findElementsByXpath(WebDriver driver) {
        List<WebElement> labelNodes;
        LOG.debug("Not able to to read workshops list, attempting to read workshops from XPath...");
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