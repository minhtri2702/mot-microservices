# Crawler Services

Dịch vụ tự động crawl dữ liệu truyện tranh từ các nguồn bên ngoài, sử dụng **Selenium WebDriver** + **Jsoup** để thu thập thông tin series và chapters.

## 🏗️ Kiến trúc tổng quan

```
┌─────────────────────────────────────────────────────────────┐
│                    Crawler Services                          │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │                  Controller Layer                     │   │
│  │  ┌────────────────────────────────────────────────┐  │   │
│  │  │         CrawlerController                       │  │   │
│  │  │         GET /test → crawlSeries()              │  │   │
│  │  └────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────┘   │
│                           │                                  │
│  ┌──────────────────────────────────────────────────────┐   │
│  │                  Service Layer                        │   │
│  │  ┌────────────────────────────────────────────────┐  │   │
│  │  │              SeleniumCrawler                    │  │   │
│  │  │  - crawlSeries()                               │  │   │
│  │  │  - crawlChaptersForSeries()                    │  │   │
│  │  │  - downloadImage()                             │  │   │
│  │  └────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────┘   │
│                           │                                  │
│  ┌──────────────────────────────────────────────────────┐   │
│  │               Repository Layer                        │   │
│  │  ┌────────────────────┐  ┌────────────────────────┐  │   │
│  │  │ CrawlSeriesRepo    │  │ CrawlChapterRepo       │  │   │
│  │  └────────────────────┘  └────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────┘   │
│                           │                                  │
│  ┌──────────────────────────────────────────────────────┐   │
│  │               Database (PostgreSQL)                   │   │
│  │  ┌──────────────┐  ┌────────────────┐  ┌──────────┐  │   │
│  │  │ crawl_series  │  │ crawl_chapters  │  │crawl_logs│  │   │
│  │  └──────────────┘  └────────────────┘  └──────────┘  │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 Công nghệ sử dụng

| Công nghệ              | Version | Mục đích                              |
|------------------------|---------|---------------------------------------|
| Spring Boot            | 3.3.5   | Framework chính                       |
| Java                   | 17      | Ngôn ngữ lập trình                    |
| Selenium               | 4.27.0  | Tự động hoá trình duyệt (crawl JS)    |
| WebDriverManager       | 5.9.2   | Tự động tải ChromeDriver              |
| Jsoup                  | 1.17.2  | Parse HTML (dự phòng)                 |
| PostgreSQL             | 15      | Cơ sở dữ liệu                        |
| JPA/Hibernate          | -       | ORM mapping                           |
| Lombok                 | 1.18.30 | Giảm boilerplate code                 |
| Spring Web             | -       | REST API                              |

## 📁 Cấu trúc thư mục

```
crawler-services/
├── pom.xml
├── Dockerfile
├── src/main/java/com/mot/crawlerservices/
│   ├── CrawlerServicesApplication.java     # Main application
│   ├── controller/
│   │   └── CrawlerController.java          # REST controller
│   ├── crawler/
│   │   ├── SeleniumCrawler.java            # Crawler chính
│   │   └── config/
│   │       └── SeleniumConfig.java         # Cấu hình ChromeDriver
│   ├── entity/
│   │   ├── CrawlSeries.java                # Entity series đã crawl
│   │   ├── CrawlChapter.java               # Entity chapter đã crawl
│   │   └── CrawlLog.java                   # Entity log crawl
│   └── repository/
│       ├── CrawlSeriesRepository.java      # Repository series
│       └── CrawlChapterRepository.java     # Repository chapter
└── src/main/resources/
    └── application.yml                     # Cấu hình ứng dụng
```

## 📄 Chi tiết các thành phần

### 1. CrawlerController (`controller/CrawlerController.java`)
```java
@RestController
@RequiredArgsConstructor
public class CrawlerController {
    private final SeleniumCrawler seleniumCrawler;

    @GetMapping("/test")
    public String test() {
        seleniumCrawler.crawlSeries();
        return null;
    }
}
```
- **Endpoint:** `GET /test`
- **Chức năng:** Kích hoạt quá trình crawl toàn bộ series từ nguồn
- **Trả về:** `null` (chỉ trigger, không cần response)

### 2. SeleniumCrawler (`crawler/SeleniumCrawler.java`)
Service chính thực hiện crawl dữ liệu.

#### Quy trình crawl:

```
1. crawlSeries()
   ├── Mở trang: https://goctruyentranhvui21.com/truyen-cap-nhat?p={page}
   ├── Chờ card series load (CSS: div.border-box.card-reader)
   ├── Scroll to bottom để load hết nội dung
   ├── Trích xuất danh sách series (title, url, externalId, imgUrl)
   ├── Với mỗi series:
   │   └── processSingleSeries()
   │       ├── Kiểm tra đã tồn tại trong DB chưa (source + externalId)
   │       ├── Nếu chưa: tải cover image → lưu vào data/{title}/cover.jpg
   │       ├── Lưu CrawlSeries vào DB (status = "PENDING")
   │       └── crawlChaptersForSeries()
   │           ├── Mở trang chi tiết series
   │           ├── Chờ danh sách chapter load (CSS: ul.list-chapters li a)
   │           ├── Với mỗi chapter:
   │           │   ├── Trích xuất chapterNumber từ text
   │           │   └── Nếu chưa tồn tại → lưu CrawlChapter (status = "NEW")
   │           └── Cập nhật series status = "DONE"
   └── Lặp lại page tiếp theo (tối đa 5 pages)
```

#### Các phương thức chính:

| Method | Mô tả |
|--------|-------|
| `crawlSeries()` | Điểm vào chính, crawl nhiều trang series |
| `extractSeriesInfo(WebElement)` | Trích xuất thông tin series từ card HTML |
| `processSingleSeries(SeriesInfo)` | Xử lý một series (lưu DB + crawl chapters) |
| `crawlChaptersForSeries(CrawlSeries)` | Crawl danh sách chapter của series |
| `extractChapterNumber(String)` | Trích số chapter từ text (VD: "Chapter 12" → 12) |
| `extractExternalId(String)` | Lấy ID từ URL (VD: `/truyen/one-piece-123` → `one-piece-123`) |
| `scrollToBottom(WebDriver)` | Scroll trang để load lazy content |
| `downloadImage(String, String)` | Tải ảnh cover về local |
| `sanitizeFileName(String)` | Loại bỏ ký tự đặc biệt khỏi tên file |

### 3. SeleniumConfig (`crawler/config/SeleniumConfig.java`)
Cấu hình ChromeDriver với các options:

```java
ChromeOptions options = new ChromeOptions();
options.addArguments("--disable-blink-features=AutomationControlled");
options.addArguments("--start-maximized");
options.addArguments("--no-sandbox");
options.addArguments("--disable-dev-shm-usage");
options.addArguments("--window-size=1920,1080");
options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) ...");
options.addArguments("--disable-gpu");
// options.addArguments("--headless"); // Bỏ comment khi deploy
```

- **Hiện tại:** Chạy có GUI (có thể thấy trình duyệt)
- **Khi deploy:** Bỏ comment `--headless` để chạy ẩn

### 4. Entities

#### CrawlSeries
| Field | Type | Ghi chú |
|-------|------|---------|
| id | UUID (PK) | Tự động sinh |
| source | String(50) | Tên nguồn (VD: "GocTruyenTranhVui") |
| externalId | String (unique) | ID từ nguồn (VD: "one-piece-123") |
| name | String | Tên truyện |
| url | String(TEXT) | URL gốc |
| status | String(20) | "PENDING" / "DONE" |
| createdAt | LocalDateTime | Thời gian tạo |

**Unique constraint:** `(source, external_id)`

#### CrawlChapter
| Field | Type | Ghi chú |
|-------|------|---------|
| id | UUID (PK) | Tự động sinh |
| seriesExternalId | String | FK đến CrawlSeries.externalId |
| chapterNumber | Integer | Số chapter |
| url | String(TEXT) | URL chapter |
| status | String(20) | "NEW" / "DONE" |
| createdAt | LocalDateTime | Thời gian tạo |

**Unique constraint:** `(series_external_id, chapter_number)`

#### CrawlLog
| Field | Type | Ghi chú |
|-------|------|---------|
| id | UUID (PK) | Tự động sinh |
| message | String(TEXT) | Nội dung log |
| createdAt | LocalDateTime | Thời gian tạo |

### 5. Repositories

#### CrawlSeriesRepository
```java
public interface CrawlSeriesRepository extends JpaRepository<CrawlSeries, UUID> {
    Optional<CrawlSeries> findBySourceAndExternalId(String source, String externalId);
}
```

#### CrawlChapterRepository
```java
public interface CrawlChapterRepository extends JpaRepository<CrawlChapter, UUID> {
    Optional<CrawlChapter> findBySeriesExternalIdAndChapterNumber(
        String seriesExternalId, Integer chapterNumber);
}
```

## 🗄️ Cơ sở dữ liệu

### crawler_db (PostgreSQL 15, port 5433)

```sql
-- Bảng series đã crawl
CREATE TABLE crawl_series (
    id UUID PRIMARY KEY,
    source VARCHAR(50),
    external_id VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    url TEXT,
    status VARCHAR(20),
    created_at TIMESTAMP,
    UNIQUE(source, external_id)
);

-- Bảng chapter đã crawl
CREATE TABLE crawl_chapters (
    id UUID PRIMARY KEY,
    series_external_id VARCHAR(255) NOT NULL,
    chapter_number INTEGER NOT NULL,
    url TEXT,
    status VARCHAR(20),
    created_at TIMESTAMP,
    UNIQUE(series_external_id, chapter_number)
);

-- Bảng log
CREATE TABLE crawl_logs (
    id UUID PRIMARY KEY,
    message TEXT,
    created_at TIMESTAMP
);
```

## 🔧 Cấu hình

### application.yml
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/crawler_db
    username: crawler_admin
    password: "0000"
  jpa:
    hibernate:
      ddl-auto: update    # Tự động tạo bảng từ Entity
    show-sql: true
server:
  port: 9090
```

### Docker Compose
```yaml
crawler-postgres:
  image: postgres:15
  container_name: crawler-postgres
  environment:
    POSTGRES_DB: crawler_db
    POSTGRES_USER: crawler_admin
    POSTGRES_PASSWORD: "0000"
  ports:
    - "5433:5432"
  volumes:
    - crawler-db-data:/var/lib/postgresql/data
```

## 🐳 Cách chạy

```bash
# 1. Khởi động database
cd mot-microservices
docker compose up -d crawler-postgres

# 2. Chạy crawler-services
cd crawler-services
./mvnw spring-boot:run

# 3. Kích hoạt crawl
curl http://localhost:9090/test
```

## 📝 Luồng dữ liệu chi tiết

```
User gọi GET /test
       │
       ▼
CrawlerController.test()
       │
       ▼
SeleniumCrawler.crawlSeries()
       │
       ├── Mở ChromeDriver (có GUI / headless)
       ├── Vào trang danh sách: /truyen-cap-nhat?p=1
       ├── Scroll to bottom → load lazy content
       ├── Lấy danh sách card series
       │
       ▼
   Với mỗi series:
       │
       ├── Kiểm tra tồn tại trong crawl_series
       │   ├── Có → bỏ qua, dùng lại
       │   └── Không → tải cover → lưu crawl_series (PENDING)
       │
       ├── Vào trang chi tiết series
       ├── Lấy danh sách chapter từ ul.list-chapters
       │
       ▼
   Với mỗi chapter:
       │
       ├── Trích xuất số chapter
       ├── Kiểm tra tồn tại trong crawl_chapters
       │   ├── Có → bỏ qua
       │   └── Không → lưu crawl_chapters (NEW)
       │
       └── Cập nhật series status = "DONE"
```

## 📝 Ghi chú phát triển

1. **Headless mode** — Hiện đang chạy có GUI. Khi deploy lên server cần bỏ comment `--headless` trong `SeleniumConfig.java`
2. **Giới hạn trang** — Hiện chỉ crawl tối đa 5 trang (`page > 5 break`). Có thể tăng hoặc bỏ giới hạn
3. **Download ảnh** — Cover image được tải về thư mục `data/{title}/cover.jpg`. Cần mount volume khi chạy Docker
4. **Chưa crawl nội dung chapter** — Hiện chỉ lưu URL chapter, chưa crawl ảnh bên trong. Cần implement thêm
5. **Không có xử lý lỗi chi tiết** — CrawlLog entity đã có nhưng chưa được dùng. Cần ghi log lỗi vào DB
6. **Không có scheduler** — Hiện crawl thủ công qua API. Có thể thêm `@Scheduled` để crawl định kỳ
7. **Anti-detection** — Đã có user-agent, disable automation flags. Có thể cần thêm proxy rotation nếu bị chặn
