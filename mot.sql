-- ============================================
-- DATABASE: mot_db (Product Database)
-- Phiên bản: 2.0 — Tối ưu cho backend
-- Mục đích: Lưu dữ liệu sản phẩm chính thức
--           đã được ETL từ crawler_db
-- ============================================

-- ============================================
-- 1. Bảng manga (truyện)
-- ============================================
CREATE TABLE IF NOT EXISTS manga (
    id UUID PRIMARY KEY,                          -- UUID từ crawler_db, giữ nguyên
    stt INTEGER UNIQUE NOT NULL,                  -- Số thứ tự (từ externalId)
    title VARCHAR(500) NOT NULL,                  -- Tên truyện
    url VARCHAR(1000),                            -- URL gốc từ nguồn crawl
    cover_image_path VARCHAR(1000),               -- Đường dẫn ảnh bìa
    status VARCHAR(50) DEFAULT 'Đang tiến hành',  -- Trạng thái: "Đang tiến hành", "Hoàn thành"
    description TEXT,                             -- Mô tả truyện
    author VARCHAR(255),                          -- Tác giả
    alternative_titles TEXT,                      -- Tên khác
    created_date VARCHAR(50),                     -- Ngày sáng tác (dạng string từ nguồn)
    translation_team VARCHAR(255),                -- Nhóm dịch
    age_rating VARCHAR(50) DEFAULT '16+',         -- Độ tuổi: "18+", "16+", ...
    views BIGINT DEFAULT 0,                       -- Lượt xem (denormalized)
    likes BIGINT DEFAULT 0,                       -- Lượt thích (denormalized)
    followers BIGINT DEFAULT 0,                   -- Lượt theo dõi (denormalized)
    max_chapter_crawled INTEGER,                  -- Chapter cao nhất đã crawl
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),  -- Thời gian tạo bản ghi
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()   -- Thời gian cập nhật
);

-- Indexes cho manga
CREATE INDEX IF NOT EXISTS idx_manga_stt ON manga(stt);
CREATE INDEX IF NOT EXISTS idx_manga_title_fts ON manga USING gin(to_tsvector('simple', title));
CREATE INDEX IF NOT EXISTS idx_manga_views ON manga(views DESC);
CREATE INDEX IF NOT EXISTS idx_manga_updated_at ON manga(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_manga_created_at ON manga(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_manga_status ON manga(status);

-- ============================================
-- 2. Bảng chapter (chương truyện)
-- ============================================
CREATE TABLE IF NOT EXISTS chapter (
    id SERIAL PRIMARY KEY,                        -- ID tự tăng
    manga_id UUID NOT NULL REFERENCES manga(id) ON DELETE CASCADE,  -- FK → manga
    chapter_number DOUBLE PRECISION NOT NULL,     -- Số chapter (hỗ trợ số thập phân: 1.5, 100.1)
    chapter_name VARCHAR(500),                    -- Tên chapter
    url VARCHAR(1000) UNIQUE,                     -- URL gốc
    view_count BIGINT DEFAULT 0,                  -- Lượt xem chapter
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),  -- Thời gian tạo
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),  -- Thời gian cập nhật (khi có chapter mới)
    UNIQUE (manga_id, chapter_number)             -- 1 manga không trùng số chapter
);

-- Indexes cho chapter
CREATE INDEX IF NOT EXISTS idx_chapter_manga_id ON chapter(manga_id);
CREATE INDEX IF NOT EXISTS idx_chapter_manga_chapter ON chapter(manga_id, chapter_number);
CREATE INDEX IF NOT EXISTS idx_chapter_updated_at ON chapter(updated_at DESC);

-- Trigger cho chapter.updated_at
DROP TRIGGER IF EXISTS trg_chapter_updated_at ON chapter;
CREATE TRIGGER trg_chapter_updated_at
    BEFORE UPDATE ON chapter
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 3. Bảng chapter_image (hình ảnh chapter)
-- ============================================
CREATE TABLE IF NOT EXISTS chapter_image (
    id SERIAL PRIMARY KEY,                        -- ID tự tăng
    chapter_id INTEGER NOT NULL REFERENCES chapter(id) ON DELETE CASCADE,  -- FK → chapter
    image_url VARCHAR(2000) NOT NULL,             -- URL ảnh (từ MinIO hoặc CDN)
    image_path VARCHAR(1000),                     -- Đường dẫn local (nếu download)
    page_order INTEGER NOT NULL,                  -- Thứ tự trang
    created_at TIMESTAMP NOT NULL DEFAULT NOW()   -- Thời gian tạo
);

-- Indexes cho chapter_image
CREATE INDEX IF NOT EXISTS idx_chapter_image_chapter_id ON chapter_image(chapter_id);
CREATE INDEX IF NOT EXISTS idx_chapter_image_order ON chapter_image(chapter_id, page_order);

-- ============================================
-- 4. Bảng genre (thể loại)
-- ============================================
CREATE TABLE IF NOT EXISTS genre (
    id SERIAL PRIMARY KEY,                        -- ID tự tăng
    name VARCHAR(100) NOT NULL UNIQUE,            -- Tên thể loại: "Action", "Romance", ...
    slug VARCHAR(100) NOT NULL UNIQUE,            -- Slug: "action", "romance", ...
    created_at TIMESTAMP NOT NULL DEFAULT NOW()   -- Thời gian tạo
);

-- ============================================
-- 5. Bảng manga_genre (n-n: truyện ↔ thể loại)
-- ============================================
CREATE TABLE IF NOT EXISTS manga_genre (
    manga_id UUID NOT NULL REFERENCES manga(id) ON DELETE CASCADE,
    genre_id INTEGER NOT NULL REFERENCES genre(id) ON DELETE CASCADE,
    PRIMARY KEY (manga_id, genre_id)
);

CREATE INDEX IF NOT EXISTS idx_manga_genre_genre_id ON manga_genre(genre_id);

-- ============================================
-- 6. Bảng users (người dùng)
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255),                           -- Email (dùng cho đăng nhập)
    phone_number VARCHAR(10),
    address VARCHAR(200) DEFAULT '',
    password VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),                      -- URL ảnh đại diện
    is_active SMALLINT DEFAULT 1,
    date_of_birth DATE,
    facebook_account_id VARCHAR(100),
    google_account_id VARCHAR(100),
    role_id INTEGER NOT NULL DEFAULT 1 REFERENCES roles(id),  -- FK → roles.id (mặc định USER=1)
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- ============================================
-- 7. Bảng roles (vai trò)
-- ============================================
CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
);

-- Insert mặc định
INSERT INTO roles (id, name) VALUES (1, 'USER') ON CONFLICT (id) DO NOTHING;
INSERT INTO roles (id, name) VALUES (2, 'ADMIN') ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 8. Bảng tokens (JWT refresh tokens)
-- ============================================
CREATE TABLE IF NOT EXISTS tokens (
    id SERIAL PRIMARY KEY,
    token VARCHAR(500) UNIQUE NOT NULL,           -- Token string (tăng độ dài)
    token_type VARCHAR(50) NOT NULL DEFAULT 'BEARER',
    expiration_date TIMESTAMP,
    revoked SMALLINT NOT NULL DEFAULT 0,
    expired SMALLINT NOT NULL DEFAULT 0,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_tokens_user_id ON tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_tokens_token ON tokens(token);

-- ============================================
-- 9. Bảng social_accounts (đăng nhập MXH)
-- ============================================
CREATE TABLE IF NOT EXISTS social_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider VARCHAR(20) NOT NULL,                -- "google", "facebook", "github"
    provider_id VARCHAR(100) NOT NULL,            -- ID từ provider
    email VARCHAR(150) NOT NULL,
    name VARCHAR(100) NOT NULL,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_social_accounts_user_id ON social_accounts(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_social_accounts_provider ON social_accounts(provider, provider_id);

-- ============================================
-- 10. Bảng user_follows (theo dõi truyện)
-- ============================================
CREATE TABLE IF NOT EXISTS user_follows (
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    manga_id UUID NOT NULL REFERENCES manga(id) ON DELETE CASCADE,
    followed_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (user_id, manga_id)
);

CREATE INDEX IF NOT EXISTS idx_user_follows_manga_id ON user_follows(manga_id);
CREATE INDEX IF NOT EXISTS idx_user_follows_user_id ON user_follows(user_id);

-- ============================================
-- 11. Bảng reading_history (lịch sử đọc)
-- ============================================
CREATE TABLE IF NOT EXISTS reading_history (
    id SERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    manga_id UUID NOT NULL REFERENCES manga(id) ON DELETE CASCADE,
    chapter_id INTEGER NOT NULL REFERENCES chapter(id) ON DELETE CASCADE,
    status VARCHAR(50) DEFAULT 'UNREAD',          -- "UNREAD", "READING", "READ"
    last_page INTEGER DEFAULT 0,                  -- Trang đang đọc (nếu có)
    last_read_date TIMESTAMP DEFAULT NOW(),
    UNIQUE (user_id, chapter_id)                  -- 1 user chỉ có 1 record/chapter
);

CREATE INDEX IF NOT EXISTS idx_reading_history_user ON reading_history(user_id);
CREATE INDEX IF NOT EXISTS idx_reading_history_manga ON reading_history(manga_id);
CREATE INDEX IF NOT EXISTS idx_reading_history_user_manga ON reading_history(user_id, manga_id);

-- ============================================
-- 12. Bảng bookmarks (đánh dấu trang)
-- ============================================
CREATE TABLE IF NOT EXISTS bookmarks (
    id SERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    manga_id UUID NOT NULL REFERENCES manga(id) ON DELETE CASCADE,
    chapter_id INTEGER NOT NULL REFERENCES chapter(id) ON DELETE CASCADE,
    page_number INTEGER DEFAULT 1,                -- Trang được đánh dấu
    note TEXT,                                    -- Ghi chú (tuỳ chọn)
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE (user_id, manga_id, chapter_id, page_number)
);

CREATE INDEX IF NOT EXISTS idx_bookmarks_user ON bookmarks(user_id);
CREATE INDEX IF NOT EXISTS idx_bookmarks_manga ON bookmarks(manga_id);

-- ============================================
-- 13. Bảng comments (bình luận)
-- ============================================
CREATE TABLE IF NOT EXISTS comments (
    comment_id SERIAL PRIMARY KEY,
    manga_id UUID REFERENCES manga(id) ON DELETE CASCADE,
    chapter_id INTEGER REFERENCES chapter(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    parent_comment_id INTEGER REFERENCES comments(comment_id) ON DELETE CASCADE,  -- Hỗ trợ reply
    comment_text TEXT NOT NULL,
    likes INTEGER DEFAULT 0,                      -- Lượt thích comment
    is_edited SMALLINT DEFAULT 0,                 -- Đã chỉnh sửa?
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_comments_manga_id ON comments(manga_id);
CREATE INDEX IF NOT EXISTS idx_comments_chapter_id ON comments(chapter_id);
CREATE INDEX IF NOT EXISTS idx_comments_user_id ON comments(user_id);
CREATE INDEX IF NOT EXISTS idx_comments_parent ON comments(parent_comment_id);

-- ============================================
-- 14. Bảng ratings (đánh giá)
-- ============================================
CREATE TABLE IF NOT EXISTS ratings (
    rating_id SERIAL PRIMARY KEY,
    manga_id UUID NOT NULL REFERENCES manga(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE (manga_id, user_id)                    -- 1 user chỉ đánh giá 1 lần/truyện
);

CREATE INDEX IF NOT EXISTS idx_ratings_manga_id ON ratings(manga_id);

-- ============================================
-- 15. Bảng notifications (thông báo)
-- ============================================
CREATE TABLE IF NOT EXISTS notifications (
    id SERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,                    -- "NEW_CHAPTER", "REPLY", "LIKE", "FOLLOW"
    title VARCHAR(255) NOT NULL,                  -- Tiêu đề thông báo
    content TEXT,                                 -- Nội dung
    reference_id VARCHAR(100),                    -- ID tham chiếu (manga_id, chapter_id, ...)
    is_read SMALLINT DEFAULT 0,                   -- Đã đọc?
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_unread ON notifications(user_id, is_read);

-- ============================================
-- 16. Bảng crawl_log (log lỗi crawl)
-- ============================================
CREATE TABLE IF NOT EXISTS crawl_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    manga_id UUID REFERENCES manga(id) ON DELETE CASCADE,
    chapter_id INTEGER REFERENCES chapter(id) ON DELETE SET NULL,
    chapter_number DOUBLE PRECISION,
    chapter_url VARCHAR(1000),
    error_type VARCHAR(100),
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    last_attempt TIMESTAMP DEFAULT NOW(),
    resolved SMALLINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_crawl_log_manga_id ON crawl_log(manga_id);
CREATE INDEX IF NOT EXISTS idx_crawl_log_resolved ON crawl_log(resolved);

-- ============================================
-- FUNCTION: Cập nhật updated_at tự động
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger cho manga
DROP TRIGGER IF EXISTS trg_manga_updated_at ON manga;
CREATE TRIGGER trg_manga_updated_at
    BEFORE UPDATE ON manga
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger cho users
DROP TRIGGER IF EXISTS trg_users_updated_at ON users;
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger cho chapter
DROP TRIGGER IF EXISTS trg_chapter_updated_at ON chapter;
CREATE TRIGGER trg_chapter_updated_at
    BEFORE UPDATE ON chapter
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- VIEW: Thống kê truyện (dùng cho listing)
-- ============================================
CREATE OR REPLACE VIEW manga_stats AS
SELECT
    m.id,
    m.stt,
    m.title,
    m.cover_image_path,
    m.status,
    m.author,
    m.views,
    m.likes,
    m.followers,
    COALESCE(AVG(r.rating), 0)::DECIMAL(3,2) AS avg_rating,
    COUNT(DISTINCT r.user_id) AS rating_count,
    MAX(c.chapter_number) AS latest_chapter,
    MAX(c.updated_at) AS latest_chapter_updated_at,
    COUNT(c.id) AS total_chapters,
    STRING_AGG(DISTINCT g.name, ', ') AS genre_names
FROM manga m
LEFT JOIN chapter c ON c.manga_id = m.id
LEFT JOIN ratings r ON r.manga_id = m.id
LEFT JOIN manga_genre mg ON mg.manga_id = m.id
LEFT JOIN genre g ON g.id = mg.genre_id
GROUP BY m.id;

-- ============================================
-- COMMENTS (Ghi chú cho các bảng)
-- ============================================
COMMENT ON TABLE manga IS 'Bảng truyện - dữ liệu sản phẩm chính thức';
COMMENT ON COLUMN manga.id IS 'UUID duy nhất của truyện (giữ nguyên từ crawler_db)';
COMMENT ON COLUMN manga.stt IS 'Số thứ tự truyện (từ externalId của nguồn crawl)';
COMMENT ON COLUMN manga.title IS 'Tên truyện';
COMMENT ON COLUMN manga.url IS 'URL gốc từ nguồn crawl';
COMMENT ON COLUMN manga.cover_image_path IS 'Đường dẫn ảnh bìa';
COMMENT ON COLUMN manga.status IS 'Trạng thái: Đang tiến hành / Hoàn thành';
COMMENT ON COLUMN manga.description IS 'Mô tả truyện';
COMMENT ON COLUMN manga.author IS 'Tác giả';
COMMENT ON COLUMN manga.alternative_titles IS 'Tên khác của truyện';
COMMENT ON COLUMN manga.created_date IS 'Ngày sáng tác (dạng string từ nguồn)';
COMMENT ON COLUMN manga.translation_team IS 'Nhóm dịch';
COMMENT ON COLUMN manga.age_rating IS 'Độ tuổi: 18+, 16+, ...';
COMMENT ON COLUMN manga.views IS 'Lượt xem (denormalized)';
COMMENT ON COLUMN manga.likes IS 'Lượt thích (denormalized)';
COMMENT ON COLUMN manga.followers IS 'Lượt theo dõi (denormalized)';
COMMENT ON COLUMN manga.max_chapter_crawled IS 'Số chapter cao nhất đã crawl được';

COMMENT ON TABLE chapter IS 'Bảng chương truyện';
COMMENT ON COLUMN chapter.id IS 'ID tự tăng của chapter';
COMMENT ON COLUMN chapter.manga_id IS 'FK → manga.id';
COMMENT ON COLUMN chapter.chapter_number IS 'Số chapter (hỗ trợ số thập phân)';
COMMENT ON COLUMN chapter.chapter_name IS 'Tên chapter';
COMMENT ON COLUMN chapter.url IS 'URL gốc của chapter';
COMMENT ON COLUMN chapter.view_count IS 'Lượt xem chapter';

COMMENT ON TABLE chapter_image IS 'Bảng hình ảnh của chapter';
COMMENT ON COLUMN chapter_image.chapter_id IS 'FK → chapter.id';
COMMENT ON COLUMN chapter_image.image_url IS 'URL của ảnh (từ MinIO/CDN)';
COMMENT ON COLUMN chapter_image.image_path IS 'Đường dẫn local (nếu download)';
COMMENT ON COLUMN chapter_image.page_order IS 'Thứ tự trang trong chapter';

COMMENT ON TABLE genre IS 'Bảng thể loại';
COMMENT ON COLUMN genre.name IS 'Tên thể loại: Action, Romance, ...';
COMMENT ON COLUMN genre.slug IS 'Slug: action, romance, ...';

COMMENT ON TABLE manga_genre IS 'Bảng liên kết truyện - thể loại (n-n)';

COMMENT ON TABLE users IS 'Bảng người dùng';
COMMENT ON COLUMN users.email IS 'Email dùng cho đăng nhập';
COMMENT ON COLUMN users.avatar_url IS 'URL ảnh đại diện';
COMMENT ON COLUMN users.role_id IS 'FK → roles.id (1=USER, 2=ADMIN)';

COMMENT ON TABLE tokens IS 'Bảng JWT refresh tokens';
COMMENT ON TABLE social_accounts IS 'Bảng tài khoản mạng xã hội';

COMMENT ON TABLE user_follows IS 'Bảng theo dõi truyện';
COMMENT ON TABLE reading_history IS 'Bảng lịch sử đọc chapter';
COMMENT ON COLUMN reading_history.status IS 'Trạng thái: UNREAD / READING / READ';
COMMENT ON COLUMN reading_history.last_page IS 'Trang đang đọc (nếu có)';

COMMENT ON TABLE bookmarks IS 'Bảng đánh dấu trang';
COMMENT ON COLUMN bookmarks.page_number IS 'Trang được đánh dấu';
COMMENT ON COLUMN bookmarks.note IS 'Ghi chú cho bookmark';

COMMENT ON TABLE comments IS 'Bảng bình luận';
COMMENT ON COLUMN comments.parent_comment_id IS 'FK → comments.comment_id (hỗ trợ reply)';
COMMENT ON COLUMN comments.likes IS 'Lượt thích comment';
COMMENT ON COLUMN comments.is_edited IS 'Đã chỉnh sửa? (0=chưa, 1=rồi)';

COMMENT ON TABLE ratings IS 'Bảng đánh giá (1-5 sao)';
COMMENT ON TABLE notifications IS 'Bảng thông báo';
COMMENT ON COLUMN notifications.type IS 'Loại: NEW_CHAPTER, REPLY, LIKE, FOLLOW';
COMMENT ON COLUMN notifications.reference_id IS 'ID tham chiếu (manga_id, chapter_id, ...)';

COMMENT ON TABLE crawl_log IS 'Bảng log lỗi crawl';
