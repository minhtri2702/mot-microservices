# MOT Microservices

Hệ thống đọc truyện tranh online (manga/comic) được xây dựng theo kiến trúc microservices với Spring Boot 3.3.5 (Java 17).

## 📚 Tài liệu

| Tài liệu | Mô tả |
|----------|-------|
| [ARCHITECTURE.md](./ARCHITECTURE.md) | Kiến trúc chi tiết toàn bộ hệ thống |
| [DATA-MAPPING.md](./DATA-MAPPING.md) | Ánh xạ dữ liệu giữa các service và database |
| [README.md](./README.md) | Tổng quan dự án (file này) |

## 🏗️ Kiến trúc tổng quan

```
┌─────────────────────────────────────────────────────────────┐
│                     API Gateway (TBD)                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────┐  ┌──────────────────┐            │
│  │  product-services    │  │ crawler-services  │            │
│  │  (Port: 8080)        │  │  (Port: 9090)     │            │
│  │  - REST API          │  │  - Selenium crawl │            │
│  │  - Manga/Chapters    │  │  - Web scraping   │            │
│  │  - Genres/Tags       │  │  - Data collection│            │
│  └────────┬─────────────┘  └────────┬──────────┘            │
│           │                         │                       │
│  ┌────────▼─────────────────────────▼──────────┐           │
│  │           commons (shared lib)               │           │
│  │  - Exception handling                        │           │
│  │  - Base response model                       │           │
│  │  - JWT security utilities                    │           │
│  │  - Correlation ID logging                    │           │
│  └──────────────────────────────────────────────┘           │
└─────────────────────────────────────────────────────────────┘
```

Xem chi tiết tại [ARCHITECTURE.md](./ARCHITECTURE.md)

## 📦 Các module

### 1. `commons` — Thư viện dùng chung
Module không chạy độc lập, được các service khác import vào.

**Chức năng:**
- **Exception Handling**: `BusinessException` (abstract), `UnprocessableEntityException`, `MasterDataIsNotFound`, `UnauthorizedException`
- **Global Exception Handler**: `ApiExceptionHandler` — `@RestControllerAdvice` bắt lỗi tập trung
- **Response Model**: `BaseResponse<T>` — format response chuẩn `{success, data, message}`
- **JWT Security**: `AuthEntryPointJwt` — xử lý lỗi 401 Unauthorized
- **Logging**: `CorrelationIdUtil` — sinh correlation ID cho tracing request

### 2. `crawler-services` — Dịch vụ crawl dữ liệu
Service tự động crawl truyện tranh từ các nguồn bên ngoài.

**Công nghệ:** Spring Boot, Selenium WebDriver 4.27.0, WebDriverManager 5.9.2, Jsoup 1.17.2, JPA/Hibernate, PostgreSQL 15

**Cấu trúc package:**
```
com.mot.crawlerservices
├── controller/
│   └── CrawlerController.java       # GET /test — kích hoạt crawl
├── crawler/
│   ├── SeleniumCrawler.java         # Crawler chính (dùng ChromeDriver)
│   └── config/
│       └── SeleniumConfig.java      # Cấu hình ChromeDriver
├── entity/
│   ├── CrawlSeries.java             # Series đã crawl
│   ├── CrawlChapter.java            # Chapter đã crawl
│   └── CrawlLog.java                # Log crawl
└── repository/
    ├── CrawlSeriesRepository.java
    └── CrawlChapterRepository.java
```

**Nguồn crawl hiện tại:** `https://goctruyentranhvui21.com`

**Database:** `crawler_db` (PostgreSQL 15, port 5433)

### 3. `product-services` — Dịch vụ sản phẩm
Service chính quản lý nội dung truyện, cung cấp REST API cho frontend.

**Công nghệ:** Spring Boot 3.3.5, JPA/Hibernate, PostgreSQL 15, Multi-DataSource

**API Endpoints:**
| Endpoint | Mô tả |
|----------|-------|
| `GET /api/v1/manga/featured` | Truyện nổi bật |
| `GET /api/v1/manga/latest-updated` | Truyện mới cập nhật |
| `GET /api/v1/manga/hot` | Truyện hot |
| `GET /api/v1/manga/new` | Truyện mới nhất |
| `GET /api/v1/manga/completed` | Truyện đã hoàn thành |
| `GET /api/v1/manga/{id}` | Chi tiết truyện |
| `GET /api/v1/manga/{mangaId}/chapters/{chapterId}` | Chi tiết chapter |
| `GET /api/v1/manga/search?keyword=` | Tìm kiếm truyện |
| `GET /api/v1/genres` | Danh sách thể loại |
| `GET /api/v1/manga/genre/{genreId}` | Truyện theo thể loại |
| `GET /api/v1/manga/{id}/related` | Truyện liên quan |

## 🗄️ Cơ sở dữ liệu

### crawler_db (crawler-services — port 5433)
| Table | Mô tả |
|-------|-------|
| `crawl_series` | Series đã crawl từ nguồn |
| `crawl_chapters` | Chapter đã crawl |
| `crawl_logs` | Log quá trình crawl |

### mot_db (product-services — port 5432)
| Table | Mô tả |
|-------|-------|
| `manga` | Truyện |
| `chapter` | Chương truyện |
| `chapter_image` | Hình ảnh chapter |
| `genre` | Thể loại |
| `manga_genre` | Liên kết truyện ↔ thể loại |
| `users` | Người dùng |
| `roles` | Vai trò (USER, ADMIN) |
| `tokens` | JWT refresh tokens |
| `social_accounts` | Đăng nhập MXH |
| `user_follows` | Theo dõi truyện |
| `user_chapter_status` | Trạng thái đọc |
| `comments` | Bình luận |
| `ratings` | Đánh giá (1-5) |
| `crawl_log` | Log lỗi crawl |

## 🐳 Docker

### docker-compose.yml (1 file duy nhất — 7 services)

```yaml
Services:
  # Infrastructure (DB + Storage)
  - mot-postgres:      PostgreSQL 15 (mot_db)      — Port 5432
  - crawler-postgres:  PostgreSQL 15 (crawler_db)  — Port 5433
  - minio:             MinIO Object Storage        — Port 9000 (API), 9001 (Console)
  - minio-init:        Tạo bucket manga-images (chạy 1 lần)
  
  # Backend Services
  - product-services:  Spring Boot (REST API)      — Port 8080
  - crawler-services:  Spring Boot (Selenium)      — Port 9090
  
  # Python Crawler
  - crawler:           Python crawler (scheduled)  — Build từ /home/minh-tri/Desktop/crawl
```

> **Lưu ý:** File `docker-compose.yml` ở `crawl/` đã được gộp vào file này.  
> Tất cả backend services đều chạy trong Docker, không cần `./mvnw spring-boot:run` nữa.

### Cách chạy

#### 🐳 Chạy toàn bộ hệ thống (khuyên dùng)
```bash
# Build & khởi động tất cả (DB + Backend + Crawler)
docker compose up -d --build

# Kiểm tra trạng thái
docker compose ps

# Xem log product-services
docker compose logs -f product-services
```

#### 🖥️ Chạy local (dev mode — không cần build Docker)
```bash
# 1. Chỉ khởi động infrastructure (DB + MinIO)
docker compose up -d mot-postgres crawler-postgres minio minio-init

# 2. Build commons (cần build trước)
cd commons && ./mvnw clean install && cd ..

# 3. Chạy crawler-services (Spring Boot)
cd crawler-services && ./mvnw spring-boot:run

# 4. Chạy product-services (terminal khác)
cd product-services && ./mvnw spring-boot:run
```

### Các lệnh Docker hữu ích
```bash
# Khởi động tất cả
docker compose up -d

# Build lại & khởi động (khi có thay đổi code)
docker compose up -d --build product-services

# Chỉ khởi động 1 service
docker compose up -d mot-postgres
docker compose up -d crawler

# Xem log
docker compose logs -f product-services
docker compose logs -f crawler

# Dừng tất cả
docker compose down

# Dừng + xoá volumes (mất dữ liệu DB)
docker compose down -v
```

## 🔧 Cấu hình

### crawler-services
- **Port:** 9090
- **DB:** localhost:5433/crawler_db

### product-services
- **Port:** 8080
- **DB:** localhost:5432/mot_db
- **Crawler DB (read-only):** localhost:5433/crawler_db

## 🔄 ETL Data Flow

```
Nguồn ngoài → crawler-services (Selenium) → crawler_db
                                                    ↓
                                          EtlService (startup)
                                                    ↓
                                              mot_db
                                                    ↓
                                          product-services (REST API)
                                                    ↓
                                              Frontend (Next.js)
```

## 📝 Ghi chú phát triển

1. **product-services** đang được phát triển — cần implement comments, ratings, users
2. **API Gateway** chưa được triển khai — các service hiện tại chạy độc lập
3. **Crawler** hiện đang chạy với GUI (headless đang comment) — cần bật `--headless` khi deploy
4. **account-service** đã được xoá — sẽ được xây dựng lại sau khi hoàn thiện product-services
5. **ETL** chạy tự động khi startup — có thể chuyển thành batch job riêng

## 🛠️ Công nghệ sử dụng

| Công nghệ | Version | Mục đích |
|-----------|---------|----------|
| Java | 17 | Ngôn ngữ lập trình |
| Spring Boot | 3.3.5 | Framework chính |
| Spring Data JPA | 3.3.5 | ORM & Database |
| Spring Security | 7.0.2 | Bảo mật |
| PostgreSQL | 15 | Cơ sở dữ liệu |
| Selenium | 4.27.0 | Web scraping |
| WebDriverManager | 5.9.2 | Quản lý ChromeDriver |
| Jsoup | 1.17.2 | HTML parsing |
| JWT (jjwt) | 0.11.5 | JWT tokens |
| Lombok | 1.18.30 | Code generation |
| MinIO | latest | Object storage |
| Docker | latest | Containerization |
