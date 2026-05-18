# 🏗️ Kiến trúc hệ thống MOT Microservices

## Tổng quan

MOT là nền tảng đọc truyện tranh online (manga/comic) được xây dựng theo kiến trúc **microservices** với **Spring Boot 3.3.5** (Java 17). Hệ thống bao gồm các service độc lập, mỗi service đảm nhận một nhiệm vụ riêng biệt và giao tiếp qua REST API.

---

## 🎯 Mục tiêu kiến trúc

1. **Tách biệt trách nhiệm** — Mỗi service chỉ quản lý một nhóm chức năng cụ thể
2. **Dễ mở rộng** — Có thể thêm service mới mà không ảnh hưởng đến service hiện tại
3. **Dễ bảo trì** — Code được tổ chức rõ ràng, module hóa
4. **Tái sử dụng** — Commons module dùng chung cho tất cả service

---

## 🏛️ Sơ đồ kiến trúc tổng thể

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Client (Frontend)                           │
│                      Next.js / React App                            │
└──────────────────────────┬──────────────────────────────────────────┘
                           │  HTTP/REST
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│                      API Gateway (TBD)                               │
│                  (Spring Cloud Gateway / Nginx)                      │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────────────────────┐   ┌──────────────────────────────┐  │
│  │     product-services        │   │     crawler-services          │  │
│  │     (Port: 8080)            │   │     (Port: 9090)              │  │
│  │                             │   │                              │  │
│  │  ┌───────────────────────┐  │   │  ┌────────────────────────┐  │  │
│  │  │   REST Controller     │  │   │  │   CrawlerController    │  │  │
│  │  │   /api/v1/*           │  │   │  │   GET /test            │  │  │
│  │  └───────────┬───────────┘  │   │  └───────────┬────────────┘  │  │
│  │              │              │   │              │               │  │
│  │  ┌───────────▼───────────┐  │   │  ┌───────────▼────────────┐  │  │
│  │  │   MangaService        │  │   │  │   SeleniumCrawler      │  │  │
│  │  │   (Business Logic)    │  │   │  │   (Web Scraping)       │  │  │
│  │  └───────────┬───────────┘  │   │  └───────────┬────────────┘  │  │
│  │              │              │   │              │               │  │
│  │  ┌───────────▼───────────┐  │   │  ┌───────────▼────────────┐  │  │
│  │  │   Repository (JPA)    │  │   │  │   Repository (JPA)     │  │  │
│  │  └───────────┬───────────┘  │   │  └───────────┬────────────┘  │  │
│  │              │              │   │              │               │  │
│  │  ┌───────────▼───────────┐  │   │  ┌───────────▼────────────┐  │  │
│  │  │   mot_db (PostgreSQL) │  │   │  │   crawler_db (PostgreSQL│  │  │
│  │  │   Port: 5432          │  │   │  │   Port: 5433            │  │  │
│  │  └───────────────────────┘  │   │  └────────────────────────┘  │  │
│  └─────────────────────────────┘   └──────────────────────────────┘  │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐    │
│  │              commons (shared library)                         │    │
│  │  - Exception handling & Global handler                        │    │
│  │  - BaseResponse<T> model                                      │    │
│  │  - JWT security (AuthEntryPointJwt)                           │    │
│  │  - Correlation ID logging                                     │    │
│  └──────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐    │
│  │              ETL Service (trong product-services)             │    │
│  │  - Đọc dữ liệu từ crawler_db (read-only)                      │    │
│  │  - Transform & migrate sang mot_db                            │    │
│  │  - Chạy tự động khi startup (@PostConstruct)                  │    │
│  └──────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 📦 Các module chi tiết

### 1. `commons` — Thư viện dùng chung

**Vai trò:** Module không chạy độc lập, được các service khác import vào.

**Cấu trúc package:**
```
com.mot
├── exception/
│   ├── BusinessException.java              # Abstract — lớp cha cho tất cả exception
│   ├── UnprocessableEntityException.java   # 422 UNPROCESSABLE_ENTITY
│   ├── MasterDataIsNotFound.java           # 302 FOUND (redirect)
│   ├── UnauthorizedException.java          # 401 UNAUTHORIZED
│   └── Model/
│       └── ApiError.java                   # {statusCode, messages[]}
├── exception/handler/
│   └── ApiExceptionHandler.java            # @RestControllerAdvice — bắt lỗi tập trung
├── response/
│   └── BaseResponse.java                   # {success, data, message}
├── security/jwt/
│   └── AuthEntryPointJwt.java              # Xử lý 401 Unauthorized
└── logging/
    └── CorrelationIdUtil.java              # Sinh UUID cho tracing request
```

**Luồng xử lý exception:**
```
Controller → Service → throws BusinessException
                              │
                              ▼
              ApiExceptionHandler (global)
                              │
                              ▼
              BaseResponse { success: false, data: ApiError, message: "..." }
```

### 2. `crawler-services` — Dịch vụ crawl dữ liệu

**Vai trò:** Tự động crawl truyện tranh từ nguồn bên ngoài bằng Selenium WebDriver.

**Công nghệ:** Spring Boot, Selenium 4.27.0, WebDriverManager 5.9.2, Jsoup 1.17.2, JPA/Hibernate, PostgreSQL 15

**Cấu trúc package:**
```
com.mot.crawlerservices
├── CrawlerServicesApplication.java         # Main class
├── controller/
│   └── CrawlerController.java              # GET /test → kích hoạt crawl
├── crawler/
│   ├── SeleniumCrawler.java                # Crawler chính (ChromeDriver)
│   └── config/
│       └── SeleniumConfig.java             # Cấu hình ChromeDriver
├── entity/
│   ├── CrawlSeries.java                    # Series đã crawl
│   ├── CrawlChapter.java                   # Chapter đã crawl
│   └── CrawlLog.java                       # Log crawl
└── repository/
    ├── CrawlSeriesRepository.java
    └── CrawlChapterRepository.java
```

**Luồng crawl:**
```
1. GET /test → CrawlerController.test()
2. SeleniumCrawler.crawlSeries()
   ├── Mở ChromeDriver (có GUI / headless)
   ├── Duyệt từng trang: /truyen-cap-nhat?p=1..5
   │   ├── Scroll to bottom (tải lazy loading)
   │   ├── Extract danh sách series (title, url, externalId, imgUrl)
   │   └── Với mỗi series:
   │       ├── Kiểm tra tồn tại trong DB
   │       ├── Nếu mới: download cover image → lưu vào data/{title}/cover.jpg
   │       └── crawlChaptersForSeries()
   │           ├── Mở trang chi tiết series
   │           ├── Lấy danh sách chapter từ ul.list-chapters
   │           └── Lưu chapter mới vào DB
   └── driver.quit()
```

**Cấu hình ChromeDriver (SeleniumConfig):**
- User-Agent giả (Windows Chrome 134)
- Tắt automation detection
- Window size: 1920x1080
- `--headless` đang comment (chạy có GUI khi dev)

### 3. `product-services` — Dịch vụ sản phẩm

**Vai trò:** Service chính quản lý nội dung truyện, cung cấp REST API cho frontend.

**Công nghệ:** Spring Boot 3.3.5, JPA/Hibernate, PostgreSQL 15, Multi-DataSource

**Cấu trúc package:**
```
com.mot.productservices
├── ProductServicesApplication.java         # Main class
├── config/
│   ├── CrawlerDbConfig.java                # DataSource thứ 2 → crawler_db (read-only)
│   └── WebConfig.java                      # Cấu hình static resources (/images/**)
├── controller/
│   └── MangaController.java                # REST API /api/v1/*
├── dto/
│   ├── MangaSummaryDTO.java                # DTO cho danh sách truyện
│   ├── MangaDetailDTO.java                 # DTO cho chi tiết truyện
│   ├── ChapterSummaryDTO.java              # DTO cho chapter trong danh sách
│   ├── ChapterDetailDTO.java               # DTO cho chi tiết chapter + ảnh
│   ├── ChapterNavigationDTO.java           # DTO cho điều hướng chapter trước/sau
│   ├── GenreDTO.java                       # DTO cho thể loại
│   └── PagedResponseDTO.java               # DTO phân trang generic
├── entity/
│   ├── Manga.java                          # Entity truyện
│   ├── Chapter.java                        # Entity chapter
│   ├── ChapterImage.java                   # Entity ảnh chapter
│   └── Genre.java                          # Entity thể loại
├── repository/
│   ├── MangaRepository.java                # Repository truyện
│   ├── ChapterRepository.java              # Repository chapter
│   ├── ChapterImageRepository.java         # Repository ảnh
│   └── GenreRepository.java                # Repository thể loại
├── service/
│   └── MangaService.java                   # Business logic
├── etl/
│   └── EtlService.java                     # ETL: crawler_db → mot_db
└── crawler/                                # Entity/Repository cho crawler_db (read-only)
    ├── entity/
    │   ├── CrawlerManga.java
    │   ├── CrawlerChapter.java
    │   ├── CrawlerChapterImage.java
    │   ├── CrawlerGenre.java
    │   └── CrawlerMangaGenre.java
    └── repository/
        ├── CrawlerMangaRepository.java
        ├── CrawlerChapterRepository.java
        ├── CrawlerChapterImageRepository.java
        ├── CrawlerGenreRepository.java
        └── CrawlerMangaGenreRepository.java
```

---

## 🔄 Luồng dữ liệu (Data Flow)

### Tổng quan 3 Database

| Database | Service | Port | Mục đích |
|----------|---------|------|----------|
| **crawler_db** | crawler-services | 5433 | Lưu dữ liệu crawl thô từ nguồn |
| **mot_db** | product-services | 5432 | Dữ liệu sản phẩm chính thức (đã xử lý) |
| **auth_db** (TBD) | account-service | TBD | Người dùng, xác thực, phân quyền |

### Luồng dữ liệu chi tiết

```
Nguồn ngoài (goctruyentranhvui21.com)
       │
       ▼  (Selenium crawl — crawler-services)
┌─────────────────────┐
│    crawler_db        │  ← Dữ liệu thô
│  - crawl_series      │
│  - crawl_chapters    │
│  - crawl_logs        │
└─────────┬───────────┘
          │  (ETL — product-services.EtlService)
          │  Chạy tự động khi product-services startup
          ▼
┌─────────────────────┐
│    mot_db            │  ← Dữ liệu chính thức
│  - manga             │
│  - chapter           │
│  - chapter_image     │
│  - genre             │
│  - manga_genre       │
│  - users             │
│  - roles             │
│  - tokens            │
│  - social_accounts   │
│  - user_follows      │
│  - user_chapter_status│
│  - comments          │
│  - ratings           │
│  - crawl_log         │
└─────────┬───────────┘
          │  (REST API — product-services.MangaController)
          ▼
┌─────────────────────┐
│    Frontend          │
│  (Next.js)           │
└─────────────────────┘
```

---

## 🌐 REST API Endpoints

### product-services (Port: 8080)

#### Danh sách truyện

| Method | Endpoint | Mô tả | Query Params |
|--------|----------|------|--------------|
| GET | `/api/v1/manga/featured` | Truyện nổi bật (top views) | — |
| GET | `/api/v1/manga/latest-updated` | Truyện mới cập nhật | `page`, `size` |
| GET | `/api/v1/manga/hot` | Truyện hot (top views có phân trang) | `page`, `size` |
| GET | `/api/v1/manga/new` | Truyện mới nhất | `page`, `size` |
| GET | `/api/v1/manga/completed` | Truyện đã hoàn thành | `page`, `size` |

#### Chi tiết

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/v1/manga/{id}` | Chi tiết truyện + chapters + genres |
| GET | `/api/v1/manga/{mangaId}/chapters/{chapterId}` | Chi tiết chapter + ảnh + điều hướng |

#### Tìm kiếm & Thể loại

| Method | Endpoint | Mô tả | Query Params |
|--------|----------|-------|--------------|
| GET | `/api/v1/manga/search` | Tìm kiếm truyện theo tên | `keyword`, `page`, `size` |
| GET | `/api/v1/genres` | Danh sách thể loại | — |
| GET | `/api/v1/manga/genre/{genreId}` | Truyện theo thể loại | `page`, `size` |
| GET | `/api/v1/manga/{id}/related` | Truyện liên quan (cùng thể loại) | `page`, `size` |

#### Static Resources

| Pattern | Mục đích |
|---------|----------|
| `/images/**` | Phục vụ ảnh tĩnh (cover, chapter images) từ `./data/` |

### crawler-services (Port: 9090)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/test` | Kích hoạt quá trình crawl |

---

## 🗄️ Cơ sở dữ liệu

### mot_db (PostgreSQL 15 — Port 5432)

Database chính cho product-services, gồm **16 bảng** chia làm 4 nhóm:

#### 🎯 Nhóm Nội dung (Content)

| Bảng | Mô tả | Ghi chú |
|------|-------|---------|
| `manga` | Truyện | UUID PK, index full-text search, denormalized views/likes/followers |
| `chapter` | Chương truyện | SERIAL PK, FK → manga, có `view_count` + `updated_at` |
| `chapter_image` | Hình ảnh chapter | SERIAL PK, FK → chapter, có `page_order` |
| `genre` | Thể loại | SERIAL PK, unique name + slug |
| `manga_genre` | Liên kết truyện ↔ thể loại | Composite PK (manga_id, genre_id) |

#### 👤 Nhóm Người dùng (User)

| Bảng | Mô tả | Ghi chú |
|------|-------|---------|
| `users` | Người dùng | UUID PK, có email + avatar_url |
| `roles` | Vai trò | USER (1), ADMIN (2) |
| `tokens` | JWT refresh tokens | FK → users, VARCHAR(500) |
| `social_accounts` | Đăng nhập MXH | FK → users, unique(provider, provider_id) |

#### 💬 Nhóm Tương tác (Interaction)

| Bảng | Mô tả | Ghi chú |
|------|-------|---------|
| `user_follows` | Theo dõi truyện | Composite PK (user_id, manga_id) |
| `reading_history` | Lịch sử đọc | Có status (UNREAD/READING/READ) + last_page |
| `bookmarks` | Đánh dấu trang | unique(user_id, manga_id, chapter_id, page_number) |
| `comments` | Bình luận | Hỗ trợ reply qua parent_comment_id |
| `ratings` | Đánh giá | rating 1-5, unique(manga_id, user_id) |

#### ⚙️ Nhóm Hệ thống (System)

| Bảng | Mô tả | Ghi chú |
|------|-------|---------|
| `notifications` | Thông báo | NEW_CHAPTER, REPLY, LIKE, FOLLOW |
| `crawl_log` | Log lỗi crawl | FK → manga, chapter |

#### 📊 View hỗ trợ

| View | Mô tả |
|------|-------|
| `manga_stats` | Thống kê truyện: avg_rating, rating_count, latest_chapter, total_chapters, genre_names |
### crawler_db (PostgreSQL 15 — Port 5433)

Database cho crawler-services, gồm 3 bảng:

| Bảng | Mô tả |
|------|-------|
| `crawl_series` | Series đã crawl (unique: source + external_id) |
| `crawl_chapters` | Chapter đã crawl (unique: series_external_id + chapter_number) |
| `crawl_logs` | Log quá trình crawl |


## 🔄 ETL Service

**Vị trí:** `product-services/src/main/java/com/mot/productservices/etl/EtlService.java`

**Chức năng:** Migrate dữ liệu từ crawler_db → mot_db khi product-services khởi động.

**Luồng ETL:**
```
1. syncGenres()
   └── Copy genre từ crawler_db.genre → mot_db.genre

2. syncManga()
   ├── Load tất cả manga từ crawler_db.manga
   ├── Load tất cả manga_genre relationships
   └── Với mỗi manga mới:
       ├── Map genres từ crawler → product
       └── Save vào mot_db.manga

3. syncChaptersAndImages()
   ├── Với mỗi manga trong crawler_db:
   │   ├── Load chapters từ crawler_db.chapter
   │   └── Với mỗi chapter mới:
   │       ├── Load images từ crawler_db.chapter_image
   │       └── Save chapter + images vào mot_db
```

**Cấu hình Multi-DataSource:**
- **Primary DataSource:** mot_db (port 5432) — dùng cho JPA repositories chính
- **Secondary DataSource:** crawler_db (port 5433) — read-only, dùng cho ETL

---

## 🐳 Docker Infrastructure

### docker-compose.yml — 5 services

| Service | Image | Port | Mục đích |
|---------|-------|------|----------|
| `mot-postgres` | postgres:15 | 5432 | mot_db |
| `crawler-postgres` | postgres:15 | 5433 | crawler_db |
| `minio` | minio/minio | 9000 (API), 9001 (Console) | Object storage cho ảnh |
| `minio-init` | minio/mc | — | Tạo bucket `manga-images` |
| `crawler` | Python (build từ `/home/minh-tri/Desktop/crawl`) | — | Python crawler (scheduled mode) |

### Volumes

| Volume | Mục đích |
|--------|----------|
| `mot-postgres-data` | Dữ liệu mot_db |
| `crawler-postgres-data` | Dữ liệu crawler_db |
| `mot-minio-data` | Dữ liệu MinIO |
| `crawler-app-data` | Dữ liệu ứng dụng crawler (images) |
| `crawler-app-logs` | Log crawler |

---

## 🔐 Security

Hiện tại hệ thống chưa có bảo mật đầy đủ:

- **commons** có sẵn `AuthEntryPointJwt` — xử lý 401 Unauthorized
- **JWT** utilities đã được chuẩn bị trong commons
- **account-service** đã bị xoá — sẽ được xây dựng lại sau
- Các API hiện tại **chưa có authentication/authorization**

---

## 🛠️ Công nghệ sử dụng

| Công nghệ | Version | Mục đích |
|-----------|---------|----------|
| Java | 17 | Ngôn ngữ lập trình |
| Spring Boot | 3.3.5 | Framework chính |
| Spring Data JPA | 3.3.5 | ORM & Database |
| Spring Security | 7.0.2 | Bảo mật (chưa dùng) |
| PostgreSQL | 15 | Cơ sở dữ liệu |
| Selenium | 4.27.0 | Web scraping |
| WebDriverManager | 5.9.2 | Quản lý ChromeDriver |
| Jsoup | 1.17.2 | HTML parsing |
| JWT (jjwt) | 0.11.5 | JWT tokens |
| Lombok | 1.18.30 | Code generation |
| MinIO | latest | Object storage |
| Docker | latest | Containerization |

---

## 📋 Hướng dẫn phát triển

### Yêu cầu
- Java 17+
- Docker & Docker Compose
- Maven (hoặc dùng `./mvnw`)
- Chrome/Chromium (cho Selenium)

### Chạy local

```bash
# 1. Khởi động database + MinIO
docker compose up -d

# 2. Build commons (cần build trước)
cd commons && ./mvnw clean install && cd ..

# 3. Chạy crawler-services
cd crawler-services && ./mvnw spring-boot:run

# 4. Chạy product-services (trong terminal khác)
cd product-services && ./mvnw spring-boot:run
```

### Ghi chú phát triển

1. **product-services** đang phát triển — cần implement thêm comments, ratings, users
2. **API Gateway** chưa triển khai — các service chạy độc lập
3. **Crawler** đang chạy GUI — cần bật `--headless` khi deploy
4. **account-service** sẽ được xây dựng lại sau khi hoàn thiện product-services
5. **ETL** chạy tự động khi startup — có thể chuyển thành batch job riêng
6. **Bảng `series` trong mot.sql** cần thêm ~10 cột để đáp ứng frontend DTO (xem DATA-MAPPING.md)

---

## 📂 Cấu trúc thư mục

```
mot-microservices/
├── commons/                          # Thư viện dùng chung
│   └── src/main/java/com/mot/
│       ├── exception/                # Business exceptions
│       ├── exception/handler/        # Global exception handler
│       ├── exception/Model/          # ApiError model
│       ├── response/                 # BaseResponse<T>
│       ├── security/jwt/             # JWT security
│       └── logging/                  # Correlation ID
├── crawler-services/                 # Dịch vụ crawl
│   └── src/main/java/com/mot/crawlerservices/
│       ├── controller/               # REST controller
│       ├── crawler/                  # Selenium crawler
│       ├── entity/                   # JPA entities
│       └── repository/               # JPA repositories
├── product-services/                 # Dịch vụ sản phẩm
│   └── src/main/java/com/mot/productservices/
│       ├── config/                   # Multi-DataSource, Web config
│       ├── controller/               # REST API controller
│       ├── dto/                      # Data Transfer Objects
│       ├── entity/                   # JPA entities (mot_db)
│       ├── repository/               # JPA repositories (mot_db)
│       ├── service/                  # Business logic
│       ├── etl/                      # ETL service
│       └── crawler/                  # Crawler DB entities/repos (read-only)
├── docker-compose.yml                # Docker infrastructure
├── mot.sql                           # Database schema (mot_db)
├── DATA-MAPPING.md                   # Data mapping documentation
├── ARCHITECTURE.md                   # This file
├── README.md                         # Project overview
└── pom.xml                           # Parent Maven POM
```
