package com.mot.crawservice.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public class CrawSeries {
    public static void main(String[] args) {

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--start-maximized");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        // options.addArguments("--headless=new");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        int page = 1;

        try {

            while (true) {

                String url = "https://goctruyentranhvui20.com/truyen-cap-nhat?p=" + page;
                System.out.println("\n===== CRAWL PAGE " + page + " =====");

                driver.get(url);

                wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("div.border-box.card-reader")
                ));

                scrollToBottom(driver);

                List<WebElement> cards =
                        driver.findElements(By.cssSelector("div.border-box.card-reader"));

                System.out.println("Cards found: " + cards.size());

                if (cards.isEmpty()) {
                    System.out.println("Không còn dữ liệu → STOP.");
                    break;
                }

                for (WebElement card : cards) {

                    try {
                        String title = card.getAttribute("title");
                        if (title == null || title.isEmpty()) continue;

                        WebElement img = card.findElement(By.cssSelector("img.v-image"));
                        String imgUrl = img.getAttribute("src");

                        if (imgUrl == null ||
                                imgUrl.contains("loading") ||
                                imgUrl.endsWith(".gif")) {
                            continue;
                        }

                        String safeTitle = sanitizeFileName(title);

                        File folder = new File("data/" + safeTitle);
                        if (!folder.exists()) folder.mkdirs();

                        File outputFile = new File(folder, "cover.jpg");

                        if (!outputFile.exists()) {
                            downloadImage(imgUrl, outputFile.getAbsolutePath());
                            System.out.println("Đã tải: " + safeTitle);
                        }

                    } catch (Exception e) {
                        System.out.println("Lỗi 1 card: " + e.getMessage());
                    }
                }

                page++;
                Thread.sleep(2000);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    // =========================
    // Scroll tới cuối trang
    // =========================
    private static void scrollToBottom(WebDriver driver) throws InterruptedException {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        long lastHeight = (long) js.executeScript("return document.body.scrollHeight");

        while (true) {
            js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
            Thread.sleep(1500);
            long newHeight = (long) js.executeScript("return document.body.scrollHeight");
            if (newHeight == lastHeight) break;
            lastHeight = newHeight;
        }
    }

    // =========================
    // Download có header để tránh 403
    // =========================
    private static void downloadImage(String imageUrl, String savePath) throws Exception {

        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36");

        connection.setRequestProperty("Referer",
                "https://goctruyentranhvui20.com/");

        connection.setRequestProperty("Accept",
                "image/avif,image/webp,image/apng,image/*,*/*;q=0.8");

        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        if (connection.getResponseCode() != 200) {
            throw new RuntimeException("HTTP " + connection.getResponseCode());
        }

        try (InputStream in = connection.getInputStream()) {
            Files.copy(in, Path.of(savePath));
        }

        connection.disconnect();
    }

    // =========================
    // Clean tên folder
    // =========================
    private static String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
