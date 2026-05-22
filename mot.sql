-- ============================================
-- MOT TRUYEN - Backend Database Schema
-- ============================================
-- Database: mot_db (PostgreSQL port 5432)
-- 
-- Kiến trúc:
--   Crawler DB (crawler_db:5433) --sync--> Backend DB (mot_db:5432) --api--> Frontend
--
-- Các nhóm bảng:
--   1. Crawl Data: manga, chapter, chapter_image, genre, manga_genre
--      (đồng bộ từ crawler DB, cấu trúc giống init_db.sql của crawler)
--   2. Auth: users, roles, social_accounts, tokens
--      (JPA auto-create từ account-service)
--   3. User Action: user_follows, user_chapter_status, comments, ratings
--      (cho product-service)
-- ============================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Enable pg_trgm extension for fuzzy search (similarity)
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- ============================================
-- 1. CRAWL DATA - Đồng bộ từ crawler DB
-- ============================================

-- Sequence for manga STT
CREATE SEQUENCE IF NOT EXISTS manga_stt_seq START 1;

-- Manga table
CREATE TABLE IF NOT EXISTS manga (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    stt INTEGER NOT NULL DEFAULT nextval('manga_stt_seq') UNIQUE,
    title VARCHAR(500) NOT NULL,
    url VARCHAR(1000) NOT NULL UNIQUE,
    cover_image_path VARCHAR(1000),
    status VARCHAR(50),
    description TEXT,
    author VARCHAR(255),
    alternative_titles TEXT,
    created_date VARCHAR(50),
    translation_team VARCHAR(255),
    age_rating VARCHAR(50),
    likes BIGINT DEFAULT 0,
    followers BIGINT DEFAULT 0,
    views BIGINT DEFAULT 0,
    max_chapter_crawled INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_manga_title ON manga(title);
CREATE INDEX IF NOT EXISTS ix_manga_url ON manga(url);
CREATE INDEX IF NOT EXISTS ix_manga_stt ON manga(stt);

-- Full-Text Search support
ALTER TABLE manga ADD COLUMN IF NOT EXISTS search_vector tsvector;

-- Hàm cập nhật search_vector tự động
CREATE OR REPLACE FUNCTION manga_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('simple', coalesce(NEW.title, '')), 'A') ||
        setweight(to_tsvector('simple', coalesce(NEW.alternative_titles, '')), 'B') ||
        setweight(to_tsvector('simple', coalesce(NEW.author, '')), 'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger tự động cập nhật search_vector khi insert/update manga
DROP TRIGGER IF EXISTS trg_manga_search_vector ON manga;
CREATE TRIGGER trg_manga_search_vector
    BEFORE INSERT OR UPDATE OF title, alternative_titles, author
    ON manga
    FOR EACH ROW
    EXECUTE FUNCTION manga_search_vector_update();

-- GIN index cho full-text search
CREATE INDEX IF NOT EXISTS ix_manga_search_vector ON manga USING GIN(search_vector);

-- Cập nhật search_vector cho dữ liệu hiện có
UPDATE manga SET search_vector =
    setweight(to_tsvector('simple', coalesce(title, '')), 'A') ||
    setweight(to_tsvector('simple', coalesce(alternative_titles, '')), 'B') ||
    setweight(to_tsvector('simple', coalesce(author, '')), 'C')
WHERE search_vector IS NULL;

COMMENT ON TABLE manga IS 'Bảng truyện - đồng bộ từ crawler DB';

-- Chapter table
CREATE TABLE IF NOT EXISTS chapter (
    id SERIAL PRIMARY KEY,
    manga_id UUID NOT NULL REFERENCES manga(id) ON DELETE CASCADE,
    chapter_number DOUBLE PRECISION NOT NULL,
    chapter_name VARCHAR(500),
    url VARCHAR(1000) NOT NULL UNIQUE,
    view_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_chapter_manga_id ON chapter(manga_id);
CREATE INDEX IF NOT EXISTS ix_chapter_manga_id_chapter_number ON chapter(manga_id, chapter_number DESC);
CREATE INDEX IF NOT EXISTS ix_chapter_url ON chapter(url);

COMMENT ON TABLE chapter IS 'Bảng chương truyện - đồng bộ từ crawler DB';

-- Chapter image table
CREATE TABLE IF NOT EXISTS chapter_image (
    id SERIAL PRIMARY KEY,
    chapter_id INTEGER NOT NULL REFERENCES chapter(id) ON DELETE CASCADE,
    image_url VARCHAR(2000) NOT NULL,
    image_path VARCHAR(1000),
    page_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_chapter_image_chapter_id ON chapter_image(chapter_id);

COMMENT ON TABLE chapter_image IS 'Bảng ảnh trong chương - đồng bộ từ crawler DB';

-- Genre table
CREATE TABLE IF NOT EXISTS genre (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_genre_name ON genre(name);
CREATE INDEX IF NOT EXISTS ix_genre_slug ON genre(slug);

COMMENT ON TABLE genre IS 'Bảng thể loại - đồng bộ từ crawler DB';

-- Manga-Genre association table
CREATE TABLE IF NOT EXISTS manga_genre (
    manga_id UUID NOT NULL REFERENCES manga(id) ON DELETE CASCADE,
    genre_id INTEGER NOT NULL REFERENCES genre(id) ON DELETE CASCADE,
    PRIMARY KEY (manga_id, genre_id)
);

CREATE INDEX IF NOT EXISTS ix_manga_genre_manga_id ON manga_genre(manga_id);
CREATE INDEX IF NOT EXISTS ix_manga_genre_genre_id ON manga_genre(genre_id);

COMMENT ON TABLE manga_genre IS 'Bảng liên kết truyện-thể loại';

-- ============================================
-- 2. AUTH - JPA auto-create từ account-service
--    (để JPA tự tạo, chỉ để tham khảo)
-- ============================================

-- Users table (account-service JPA sẽ tự tạo)
-- CREATE TABLE IF NOT EXISTS users (
--     id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
--     email VARCHAR(255) NOT NULL UNIQUE,
--     full_name VARCHAR(255),
--     password VARCHAR(255),
--     avatar_url VARCHAR(255),
--     is_active BOOLEAN DEFAULT TRUE,
--     created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
--     updated_at TIMESTAMP WITH TIME ZONE
-- );

-- Roles table (account-service JPA sẽ tự tạo)
-- CREATE TABLE IF NOT EXISTS roles (
--     id INTEGER PRIMARY KEY,
--     name VARCHAR(20) NOT NULL
-- );

-- Users-Roles join table (account-service JPA sẽ tự tạo)
-- CREATE TABLE IF NOT EXISTS users_roles (
--     user_id UUID NOT NULL REFERENCES users(id),
--     role_id INTEGER NOT NULL REFERENCES roles(id),
--     PRIMARY KEY (user_id, role_id)
-- );

-- Social accounts table (account-service JPA sẽ tự tạo)
-- CREATE TABLE IF NOT EXISTS social_accounts (
--     id BIGSERIAL PRIMARY KEY,
--     user_id UUID REFERENCES users(id),
--     provider VARCHAR(255),
--     provider_id VARCHAR(255),
--     linked_at TIMESTAMP WITH TIME ZONE
-- );

-- ============================================
-- 3. USER ACTION - Cho product-service
-- ============================================

-- User follows table
CREATE TABLE IF NOT EXISTS user_follows (
    user_id UUID NOT NULL,
    manga_id UUID NOT NULL REFERENCES manga(id) ON DELETE CASCADE,
    followed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (user_id, manga_id)
);

CREATE INDEX IF NOT EXISTS ix_user_follows_user_id ON user_follows(user_id);
CREATE INDEX IF NOT EXISTS ix_user_follows_manga_id ON user_follows(manga_id);

COMMENT ON TABLE user_follows IS 'Bảng người dùng theo dõi truyện';
COMMENT ON COLUMN user_follows.user_id IS 'FK tới bảng users (account-service)';

-- User chapter reading status
CREATE TABLE IF NOT EXISTS user_chapter_status (
    user_id UUID NOT NULL,
    chapter_id INTEGER NOT NULL REFERENCES chapter(id) ON DELETE CASCADE,
    status VARCHAR(50) DEFAULT 'unread',
    last_read_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (user_id, chapter_id)
);

COMMENT ON TABLE user_chapter_status IS 'Bảng trạng thái đọc chương của người dùng';
COMMENT ON COLUMN user_chapter_status.status IS 'Trạng thái: unread, reading, read';

-- Comments table
CREATE TABLE IF NOT EXISTS comments (
    comment_id SERIAL PRIMARY KEY,
    chapter_id INTEGER REFERENCES chapter(id) ON DELETE CASCADE,
    manga_id UUID REFERENCES manga(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    parent_comment_id INTEGER REFERENCES comments(comment_id) ON DELETE CASCADE,
    comment_text TEXT NOT NULL,
    like_count INTEGER DEFAULT 0,
    reply_count INTEGER DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_comments_chapter_id ON comments(chapter_id);
CREATE INDEX IF NOT EXISTS ix_comments_manga_id ON comments(manga_id);
CREATE INDEX IF NOT EXISTS ix_comments_user_id ON comments(user_id);

COMMENT ON TABLE comments IS 'Bảng bình luận';

-- Comment likes table
CREATE TABLE IF NOT EXISTS comment_likes (
    id BIGSERIAL PRIMARY KEY,
    comment_id INTEGER NOT NULL REFERENCES comments(comment_id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE (comment_id, user_id)
);

CREATE INDEX IF NOT EXISTS ix_comment_likes_comment_id ON comment_likes(comment_id);
CREATE INDEX IF NOT EXISTS ix_comment_likes_user_id ON comment_likes(user_id);

COMMENT ON TABLE comment_likes IS 'Bảng like bình luận';

-- Ratings table
CREATE TABLE IF NOT EXISTS ratings (
    rating_id SERIAL PRIMARY KEY,
    manga_id UUID NOT NULL REFERENCES manga(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    rating INTEGER CHECK (rating >= 1 AND rating <= 5),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE (manga_id, user_id)
);

CREATE INDEX IF NOT EXISTS ix_ratings_manga_id ON ratings(manga_id);
CREATE INDEX IF NOT EXISTS ix_ratings_user_id ON ratings(user_id);

COMMENT ON TABLE ratings IS 'Bảng đánh giá truyện';

-- ============================================
-- SEED DATA
-- ============================================

-- Insert default genres (nếu chưa có)
INSERT INTO genre (name, slug) VALUES
    ('Action', 'action'),
    ('Adventure', 'adventure'),
    ('Comedy', 'comedy'),
    ('Drama', 'drama'),
    ('Fantasy', 'fantasy'),
    ('Horror', 'horror'),
    ('Mystery', 'mystery'),
    ('Romance', 'romance'),
    ('School Life', 'school-life'),
    ('Sci-Fi', 'sci-fi'),
    ('Seinen', 'seinen'),
    ('Shoujo', 'shoujo'),
    ('Shounen', 'shounen'),
    ('Slice of Life', 'slice-of-life'),
    ('Sports', 'sports'),
    ('Supernatural', 'supernatural'),
    ('Tragedy', 'tragedy'),
    ('Webtoon', 'webtoon')
ON CONFLICT (slug) DO NOTHING;
