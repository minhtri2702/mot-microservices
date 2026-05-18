# Ánh xạ dữ liệu giữa các service

Tài liệu này mô tả cách dữ liệu được crawl từ nguồn bên ngoài, lưu vào **crawler_db**, sau đó chuyển đổi và lưu vào **mot_db**, và cuối cùng trả về qua API cho **frontend**.

---

## 🗄️ Tổng quan 3 Database

| Database | Service | Port | Mục đích |
|----------|---------|------|----------|
| **crawler_db** | crawler-services | 5433 | Lưu dữ liệu crawl thô từ nguồn |
| **mot_db** | product-services | 5432 | Dữ liệu sản phẩm chính thức (đã xử lý) |
| **auth_db** (TBD) | account-service | TBD | Người dùng, xác thực, phân quyền |

---

## 🔄 Luồng dữ liệu tổng thể

```
Nguồn ngoài (goctruyentranhvui21.com)
       │
       ▼  (Selenium crawl — crawler-services)
┌─────────────────────┐
│    crawler_db        │  ← Dữ liệu thô, tạm thời
│  - crawl_series      │
│  - crawl_chapters    │
│  - crawl_logs        │
└─────────┬───────────┘
          │  (ETL: transform & migrate — product-services.EtlService)
          │  Chạy tự động khi product-services startup
          ▼
┌──────────────────────────────────────────┐
│              mot_db                       │  ← Dữ liệu chính thức
│                                           │
│  ┌─────────────────────────────────────┐  │
│  │  NHÓM NỘI DUNG (Content)            │  │
│  │  ├── manga                          │  │
│  │  ├── chapter                        │  │
│  │  ├── chapter_image                  │  │
│  │  ├── genre                          │  │
│  │  └── manga_genre                    │  │
│  ├─────────────────────────────────────┤  │
│  │  NHÓM NGƯỜI DÙNG (User)            │  │
│  │  ├── users                          │  │
│  │  ├── roles                          │  │
│  │  ├── tokens                         │  │
│  │  └── social_accounts                │  │
│  ├─────────────────────────────────────┤  │
│  │  NHÓM TƯƠNG TÁC (Interaction)      │  │
│  │  ├── user_follows                   │  │
│  │  ├── reading_history                │  │
│  │  ├── bookmarks                      │  │
│  │  ├── comments                       │  │
│  │  └── ratings                        │  │
│  ├─────────────────────────────────────┤  │
│  │  NHÓM HỆ THỐNG (System)            │  │
│  │  ├── notifications                  │  │
│  │  └── crawl_log                      │  │
│  └─────────────────────────────────────┘  │
└─────────────────────┬────────────────────┘
                      │  (REST API — product-services.MangaController)
                      ▼
┌─────────────────────┐
│    Frontend          │
│  (Next.js)           │
└─────────────────────┘
```

---

## 📊 Ánh xạ chi tiết: crawler_db → mot_db → Frontend API

### 1. Manga (Truyện)

| crawler_db (crawl_series) | mot_db (manga) | Frontend DTO | Ghi chú |
|---|---|---|---|
| `id` (UUID) | `id` (UUID) | `id` | Giữ nguyên UUID |
| `name` | `title` | `title` | Tên truyện |
| `url` | `url` | — | URL gốc từ nguồn crawl |
| `externalId` (VD: "one-piece-123") | `stt` (INTEGER) | `stt` | Parse số từ externalId |
| — | `status` | `status` | "Đang tiến hành" / "Hoàn thành" |
| — | `description` | `description` | Mô tả (cần crawl thêm) |
| `imgUrl` → download → `data/{name}/cover.jpg` | `cover_image_path` | `coverImagePath` | Đường dẫn ảnh bìa |
| — | `author` | `author` | Tác giả (cần crawl thêm) |
| — | `alternative_titles` | `alternativeTitles` | Tên khác |
| — | `created_date` | `createdDate` | Ngày sáng tác |
| — | `translation_team` | `translationTeam` | Nhóm dịch |
| — | `age_rating` | `ageRating` | Độ tuổi |
| — | `views` | `views` | Lượt xem (denormalized) |
| — | `likes` | `likes` | Lượt thích (denormalized) |
| — | `followers` | `followers` | Lượt theo dõi (denormalized) |
| — | `max_chapter_crawled` | — | Chapter cao nhất đã crawl |
| `createdAt` | `created_at` | — | Thời gian tạo |
| — | `updated_at` | — | Thời gian cập nhật |

### 2. Chapter (Chương truyện)

| crawler_db (crawl_chapters) | mot_db (chapter) | Frontend DTO | Ghi chú |
|---|---|---|---|
| `id` (UUID) | `id` (SERIAL) | `id` | Kiểu khác nhau (UUID vs Integer) |
| `seriesExternalId` | `manga_id` (FK → manga) | — | Cần map externalId → manga.id |
| `chapterNumber` | `chapter_number` | `chapterNumber` | Số chapter (Double) |
| — | `chapter_name` | `chapterName` | Tên chapter |
| `url` | `url` | — | URL gốc |
| — | `view_count` | `viewCount` | Lượt xem (mặc định 0) |
| `createdAt` | `created_at` | `createdAt` | Thời gian tạo |
| — | `updated_at` | — | Thời gian cập nhật |

### 3. ChapterImage (Hình ảnh chapter)

| crawler_db | mot_db (chapter_image) | Frontend DTO | Ghi chú |
|---|---|---|---|
| — | `id` (SERIAL) | — | PK |
| — | `chapter_id` (FK → chapter) | — | FK đến chapter |
| — | `image_url` | `imageUrls[]` | URL ảnh (từ MinIO/CDN) |
| — | `image_path` | — | Đường dẫn local |
| — | `page_order` | — | Thứ tự trang |
| — | `created_at` | — | Thời gian tạo |

### 4. Genre (Thể loại)

| crawler_db | mot_db (genre) | Frontend DTO | Ghi chú |
|---|---|---|---|
| — | `id` (SERIAL) | `id` | PK |
| — | `name` | `name` | Tên thể loại (VD: "Action") |
| — | `slug` | `slug` | Slug từ tên (VD: "action") |

### 5. Manga_Genre (Liên kết n-n)

| crawler_db | mot_db (manga_genre) | Ghi chú |
|---|---|---|
| — | `manga_id` (FK → manga) | Composite PK |
| — | `genre_id` (FK → genre) | Composite PK |

---

## 🔗 Ánh xạ API endpoints → Database queries

### GET /api/v1/manga/featured
```sql
-- Lấy truyện nổi bật (top views)
SELECT m.id, m.title, m.cover_image_path, m.status, m.author,
       m.views, m.likes, m.followers,
       MAX(c.chapter_number) AS latest_chapter,
       MAX(c.updated_at) AS latest_chapter_updated_at,
       STRING_AGG(DISTINCT g.name, ', ') AS genres
FROM manga m
LEFT JOIN chapter c ON c.manga_id = m.id
LEFT JOIN manga_genre mg ON mg.manga_id = m.id
LEFT JOIN genre g ON g.id = mg.genre_id
GROUP BY m.id
ORDER BY m.views DESC
LIMIT 10;
```

### GET /api/v1/manga/latest-updated
```sql
-- Truyện có chapter mới nhất
SELECT m.*, MAX(c.updated_at) AS latest_update
FROM manga m
JOIN chapter c ON c.manga_id = m.id
GROUP BY m.id
ORDER BY latest_update DESC;
```

### GET /api/v1/manga/{id}
```sql
-- Chi tiết truyện + chapters + genres
SELECT m.*, 
       STRING_AGG(DISTINCT g.name, ', ') AS genres,
       COALESCE(AVG(r.rating), 0)::DECIMAL(3,2) AS avg_rating
FROM manga m
LEFT JOIN manga_genre mg ON mg.manga_id = m.id
LEFT JOIN genre g ON g.id = mg.genre_id
LEFT JOIN ratings r ON r.manga_id = m.id
WHERE m.id = :id
GROUP BY m.id;
```

### GET /api/v1/manga/{mangaId}/chapters/{chapterId}
```sql
-- Nội dung chapter + ảnh
SELECT c.*, ci.image_url, ci.page_order
FROM chapter c
LEFT JOIN chapter_image ci ON ci.chapter_id = c.id
WHERE c.manga_id = :mangaId AND c.id = :chapterId
ORDER BY ci.page_order ASC;
```

### GET /api/v1/genres
```sql
SELECT id, name, slug
FROM genre
ORDER BY name;
```

### GET /api/v1/manga/genre/{genreId}
```sql
SELECT m.* FROM manga m
JOIN manga_genre mg ON mg.manga_id = m.id
WHERE mg.genre_id = :genreId
ORDER BY m.updated_at DESC;
```

### GET /api/v1/manga/search
```sql
-- Full-text search
SELECT * FROM manga
WHERE to_tsvector('simple', title) @@ plainto_tsquery('simple', :keyword)
   OR title ILIKE '%' || :keyword || '%'
ORDER BY updated_at DESC;
```

---

## 🧩 Frontend DTO → Database Column Mapping

### MangaSummaryDTO
```typescript
interface MangaSummaryDTO {
  id: string;                    // manga.id
  stt: number;                   // manga.stt
  title: string;                 // manga.title
  coverImagePath: string;        // manga.cover_image_path
  status: string;                // manga.status
  author: string;                // manga.author
  views: number;                 // manga.views
  likes: number;                 // manga.likes
  followers: number;             // manga.followers
  latestChapter: number;         // MAX(chapter.chapter_number)
  latestChapterUpdatedAt: string | null; // MAX(chapter.updated_at)
  genres: string[];              // genre.name (JOIN qua manga_genre)
}
```

### MangaDetailDTO
```typescript
interface MangaDetailDTO {
  id: string;                    // manga.id
  stt: number;                   // manga.stt
  title: string;                 // manga.title
  coverImagePath: string;        // manga.cover_image_path
  status: string;                // manga.status
  description: string;           // manga.description
  author: string;                // manga.author
  alternativeTitles: string;     // manga.alternative_titles
  createdDate: string;           // manga.created_date
  translationTeam: string;       // manga.translation_team
  ageRating: string;             // manga.age_rating
  likes: number;                 // manga.likes
  followers: number;             // manga.followers
  views: number;                 // manga.views
  realViews: number;             // manga.views (có thể tính riêng sau)
  latestChapter: number;         // MAX(chapter.chapter_number)
  latestChapterUpdatedAt: string; // MAX(chapter.updated_at)
  genres: string[];              // genre.name (JOIN)
  chapters: ChapterSummaryDTO[]; // chapter (JOIN)
}
```

### ChapterDetailDTO
```typescript
interface ChapterDetailDTO {
  id: number;                    // chapter.id
  chapterNumber: number;         // chapter.chapter_number
  chapterName: string;           // chapter.chapter_name
  viewCount: number;             // chapter.view_count
  createdAt: string;             // chapter.created_at
  imageUrls: string[];           // chapter_image.image_url (JOIN)
  navigation: {                  // Tính từ chapter cùng manga
    prevChapterId: number | null;
    prevChapterNumber: number | null;
    nextChapterId: number | null;
    nextChapterNumber: number | null;
  };
}
```

---

## 📋 So sánh: Database cũ (v1) vs Database mới (v2)

| Thay đổi | v1 (cũ) | v2 (mới) | Lý do |
|----------|---------|----------|-------|
| **Bảng `user_chapter_status`** | ✅ Có | ❌ Xoá | Thay bằng `reading_history` linh hoạt hơn |
| **Bảng `reading_history`** | ❌ Không | ✅ Thêm | Lưu lịch sử đọc + last_page + status |
| **Bảng `bookmarks`** | ❌ Không | ✅ Thêm | Đánh dấu trang cụ thể |
| **Bảng `notifications`** | ❌ Không | ✅ Thêm | Thông báo cho người dùng |
| **Chapter có `view_count`** | ❌ Không | ✅ Có | Đếm lượt xem chapter |
| **Chapter có `updated_at`** | ❌ Không | ✅ Có | Biết chapter nào mới cập nhật |
| **Comments có `parent_comment_id`** | ❌ Không | ✅ Có | Hỗ trợ reply comment |
| **Comments có `likes` + `is_edited`** | ❌ Không | ✅ Có | Tương tác comment tốt hơn |
| **Users có `email` + `avatar_url`** | ❌ Không | ✅ Có | Đăng nhập bằng email, avatar |
| **Users có `role_id` FK chuẩn** | ❌ DEFAULT 0 | ✅ REFERENCES roles(id) | Ràng buộc khoá ngoại đúng |
| **Tokens có độ dài 500** | ❌ VARCHAR(255) | ✅ VARCHAR(500) | JWT token dài hơn |
| **Social accounts unique index** | ❌ Không | ✅ (provider, provider_id) | Tránh trùng lặp |
| **View `manga_stats`** | ❌ Không | ✅ Có | Query listing nhanh hơn |
| **Trigger `chapter.updated_at`** | ❌ Không | ✅ Có | Tự động cập nhật thời gian |

---

## 🗺️ Tóm tắt ánh xạ giữa các DB

```
crawler_db (thô)              mot_db (chính thức)               Frontend
─────────────────              ─────────────────────             ────────
crawl_series.id         ───→  manga.id                    ───→  id
crawl_series.name       ───→  manga.title                 ───→  title
crawl_series.externalId ───→  manga.stt                   ───→  stt
(cover downloaded)      ───→  manga.cover_image_path      ───→  coverImagePath
(crawl thêm)            ───→  manga.status                ───→  status
(crawl thêm)            ───→  manga.description           ───→  description
(crawl thêm)            ───→  manga.author                ───→  author
(crawl thêm)            ───→  genre.name                  ───→  genres[]
(crawl thêm)            ───→  manga.alternative_titles    ───→  alternativeTitles
(crawl thêm)            ───→  manga.translation_team      ───→  translationTeam
(crawl thêm)            ───→  manga.age_rating            ───→  ageRating

crawl_chapters.chapterNumber ───→  chapter.chapter_number  ───→  chapterNumber
crawl_chapters.url      ───→  chapter.url                 ───→  (dùng để crawl ảnh)
(crawl ảnh thêm)        ───→  chapter_image.image_url     ───→  imageUrls[]
```

---

## 📝 Kết luận

1. **crawler_db và mot_db là 2 database riêng biệt** — không dùng chung
2. **Crawler hiện tại mới crawl được:** tên truyện, URL, externalId, cover image, danh sách chapter
3. **Cần crawl thêm:** thể loại, tác giả, mô tả, ảnh chapter, trạng thái, tên khác
4. **ETL service** migrate dữ liệu từ crawler_db → mot_db khi startup
5. **mot_db v2** có 16 bảng (tăng 2 bảng so với v1): thêm `reading_history`, `bookmarks`, `notifications`
6. **Users/Auth** nên tách riêng thành auth_db cho account-service trong tương lai
