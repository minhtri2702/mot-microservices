package com.mot.crawlerservices.crawler;

import com.mot.crawlerservices.entity.CrawlChapter;
import com.mot.crawlerservices.entity.CrawlSeries;
import com.mot.crawlerservices.repository.CrawlChapterRepository;
import com.mot.crawlerservices.repository.CrawlSeriesRepository;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class SeleniumCrawler {

    private final ChromeDriver driver;
    private final CrawlSeriesRepository crawlSeriesRepository;
    private final CrawlChapterRepository crawlChapterRepository;

    private static final String SOURCE_NAME = "GocTruyenTranhVui";
    private static final String BASE_URL = "https://goctruyentranhvui21.com";

    @Autowired
    public SeleniumCrawler(ChromeDriver chromeDriver, 
                           CrawlSeriesRepository crawlSeriesRepository, 
                           CrawlChapterRepository crawlChapterRepository) {
        this.driver = chromeDriver;
        this.crawlSeriesRepository = crawlSeriesRepository;
        this.crawlChapterRepository = crawlChapterRepository;
    }

    public void crawlSeries() {
        if (driver == null) {
            log.error("Driver is null. Check SeleniumConfig.");
            return;
        }

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        int page = 1;
        try {
            while (true) {
                String url = BASE_URL + "/truyen-cap-nhat?p=" + page;
                log.info("===== CRAWLING PAGE {} =====", page);
                driver.get(url);
                
                wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("div.border-box.card-reader")
                ));
                
                scrollToBottom(driver);
                
                List<WebElement> cards = driver.findElements(By.cssSelector("div.border-box.card-reader"));
                log.info("Cards found: {}", cards.size());
                
                if (cards.isEmpty()) {
                    log.info("No more data → STOP.");
                    break;
                }

                List<SeriesInfo> seriesList = new ArrayList<>();
                for (WebElement card : cards) {
                    SeriesInfo info = extractSeriesInfo(card);
                    if (info != null) seriesList.add(info);
                }

                for (SeriesInfo info : seriesList) {
                    processSingleSeries(info);
                }
                
                page++;
                if (page > 5) break; 
                Thread.sleep(2000);
            }
        } catch (Exception e) {
            log.error("Error during crawling: ", e);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private SeriesInfo extractSeriesInfo(WebElement card) {
        try {
            String title = card.getAttribute("title");
            if (title == null || title.isEmpty()) return null;

            WebElement linkElement = card.findElement(By.cssSelector("a"));
            String sourceUrl = linkElement.getAttribute("href");
            String externalId = extractExternalId(sourceUrl);

            WebElement img = card.findElement(By.cssSelector("img.v-image"));
            String imgUrl = img.getAttribute("src");

            return new SeriesInfo(title, sourceUrl, externalId, imgUrl);
        } catch (Exception e) {
            return null;
        }
    }

    private void processSingleSeries(SeriesInfo info) {
        try {
            Optional<CrawlSeries> existing = crawlSeriesRepository.findBySourceAndExternalId(SOURCE_NAME, info.externalId);
            CrawlSeries series;
            if (existing.isEmpty()) {
                String safeTitle = sanitizeFileName(info.title);
                File folder = new File("data/" + safeTitle);
                if (!folder.exists()) folder.mkdirs();
                File outputFile = new File(folder, "cover.jpg");

                if (!outputFile.exists() && info.imgUrl != null && !info.imgUrl.contains("loading")) {
                    downloadImage(info.imgUrl, outputFile.getAbsolutePath());
                }

                series = CrawlSeries.builder()
                        .source(SOURCE_NAME)
                        .externalId(info.externalId)
                        .name(info.title)
                        .url(info.sourceUrl)
                        .status("PENDING")
                        .createdAt(LocalDateTime.now())
                        .build();
                series = crawlSeriesRepository.save(series);
                log.info("Saved Series: {}", info.title);
            } else {
                series = existing.get();
                log.info("Series {} already exists.", info.title);
            }

            crawlChaptersForSeries(series);

        } catch (Exception e) {
            log.error("Error processing series {}: {}", info.title, e.getMessage());
        }
    }

    private void crawlChaptersForSeries(CrawlSeries series) {
        try {
            driver.get(series.getUrl());
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("ul.list-chapters")));

            List<WebElement> chapterElements = driver.findElements(By.cssSelector("ul.list-chapters li a"));
            log.info("Found {} chapters for {}", chapterElements.size(), series.getName());

            for (WebElement el : chapterElements) {
                String chapterUrl = el.getAttribute("href");
                String chapterTitle = el.getText();
                Integer chapterNumber = extractChapterNumber(chapterTitle);

                if (chapterNumber == null) continue;

                Optional<CrawlChapter> existingChapter = crawlChapterRepository
                        .findBySeriesExternalIdAndChapterNumber(series.getExternalId(), chapterNumber);

                if (existingChapter.isEmpty()) {
                    CrawlChapter chapter = CrawlChapter.builder()
                            .seriesExternalId(series.getExternalId())
                            .chapterNumber(chapterNumber)
                            .url(chapterUrl)
                            .status("NEW")
                            .createdAt(LocalDateTime.now())
                            .build();
                    crawlChapterRepository.save(chapter);
                }
            }
            
            series.setStatus("DONE");
            crawlSeriesRepository.save(series);

        } catch (Exception e) {
            log.warn("Could not crawl chapters for {}: {}", series.getName(), e.getMessage());
        }
    }

    private Integer extractChapterNumber(String text) {
        try {
            String numberOnly = text.replaceAll("[^0-9]", "");
            return Integer.parseInt(numberOnly);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractExternalId(String url) {
        if (url == null || !url.contains("/")) return "unknown";
        String[] parts = url.split("/");
        return parts[parts.length - 1];
    }

    private void scrollToBottom(WebDriver driver) throws InterruptedException {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        long lastHeight = (long) js.executeScript("return document.body.scrollHeight");
        while (true) {
            js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
            Thread.sleep(1000);
            long newHeight = (long) js.executeScript("return document.body.scrollHeight");
            if (newHeight == lastHeight) break;
            lastHeight = newHeight;
        }
    }

    private void downloadImage(String imageUrl, String savePath) throws Exception {
        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36");
        connection.setRequestProperty("Referer", BASE_URL + "/");
        connection.setConnectTimeout(10000);
        if (connection.getResponseCode() == 200) {
            try (InputStream in = connection.getInputStream()) {
                Files.copy(in, Path.of(savePath));
            }
        }
        connection.disconnect();
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "").replaceAll("\\s+", " ").trim();
    }

    @lombok.AllArgsConstructor
    private static class SeriesInfo {
        String title, sourceUrl, externalId, imgUrl;
    }
}
