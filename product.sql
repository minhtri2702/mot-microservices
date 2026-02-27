-- =========================
-- EXTENSION
-- =========================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =========================
-- SERIES (TRUYỆN)
-- =========================
CREATE TABLE series (
                        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                        name TEXT NOT NULL,
                        description TEXT,
                        status SMALLINT, -- 0:draft, 1:ongoing, 2:completed
                        cover_image_url TEXT,
                        created_at TIMESTAMP DEFAULT now(),
                        updated_at TIMESTAMP DEFAULT now()
);

-- =========================
-- AUTHORS
-- ==========================
CREATE TABLE authors (
                         id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                         name TEXT NOT NULL
);

CREATE TABLE series_authors (
                                series_id UUID REFERENCES series(id) ON DELETE CASCADE,
                                author_id UUID REFERENCES authors(id) ON DELETE CASCADE,
                                PRIMARY KEY (series_id, author_id)
);

-- =========================
-- GENRES
-- =========================
CREATE TABLE genres (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE series_genres (
                               series_id UUID REFERENCES series(id) ON DELETE CASCADE,
                               genre_id INT REFERENCES genres(id) ON DELETE CASCADE,
                               PRIMARY KEY (series_id, genre_id)
);

-- =========================
-- TAGS
-- =========================
CREATE TABLE tags (
                      id SERIAL PRIMARY KEY,
                      name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE series_tags (
                             series_id UUID REFERENCES series(id) ON DELETE CASCADE,
                             tag_id INT REFERENCES tags(id) ON DELETE CASCADE,
                             PRIMARY KEY (series_id, tag_id)
);

-- =========================
-- CHAPTERS
-- =========================
CREATE TABLE chapters (
                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          series_id UUID NOT NULL REFERENCES series(id) ON DELETE CASCADE,
                          chapter_number INT NOT NULL,
                          title TEXT,
                          content_url TEXT,
                          release_date DATE,
                          view_count BIGINT DEFAULT 0,
                          created_at TIMESTAMP DEFAULT now(),
                          updated_at TIMESTAMP DEFAULT now(),
                          UNIQUE (series_id, chapter_number)
);

-- =========================
-- IMAGES (ẢNH CHƯƠNG)
-- =========================
CREATE TABLE images (
                        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                        chapter_id UUID NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
                        image_url TEXT NOT NULL,
                        image_order INT,
                        created_at TIMESTAMP DEFAULT now()
);

-- =========================
-- USERS & AUTH
-- =========================
CREATE TABLE roles (
                       id SERIAL PRIMARY KEY,
                       name VARCHAR(20) UNIQUE NOT NULL
);

CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       username VARCHAR(100),
                       phone_number VARCHAR(20),
                       address TEXT,
                       password VARCHAR(255) NOT NULL,
                       role_id INT REFERENCES roles(id),
                       is_active BOOLEAN DEFAULT TRUE,
                       date_of_birth DATE,
                       created_at TIMESTAMP DEFAULT now(),
                       updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE social_accounts (
                                 id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                 provider VARCHAR(20) NOT NULL, -- google, facebook
                                 provider_id VARCHAR(100) NOT NULL,
                                 email VARCHAR(150),
                                 name VARCHAR(100),
                                 user_id UUID REFERENCES users(id) ON DELETE CASCADE,
                                 UNIQUE (provider, provider_id)
);

CREATE TABLE tokens (
                        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                        token TEXT UNIQUE NOT NULL,
                        token_type VARCHAR(20),
                        expired BOOLEAN DEFAULT FALSE,
                        revoked BOOLEAN DEFAULT FALSE,
                        expiration_date TIMESTAMP,
                        user_id UUID REFERENCES users(id) ON DELETE CASCADE
);

-- =========================
-- USER INTERACTION
-- =========================
CREATE TABLE user_follows (
                              user_id UUID REFERENCES users(id) ON DELETE CASCADE,
                              series_id UUID REFERENCES series(id) ON DELETE CASCADE,
                              followed_at TIMESTAMP DEFAULT now(),
                              PRIMARY KEY (user_id, series_id)
);

CREATE TABLE user_chapter_status (
                                     user_id UUID REFERENCES users(id) ON DELETE CASCADE,
                                     chapter_id UUID REFERENCES chapters(id) ON DELETE CASCADE,
                                     status SMALLINT, -- 0:unread, 1:reading, 2:completed
                                     last_read_at TIMESTAMP,
                                     PRIMARY KEY (user_id, chapter_id)
);

CREATE TABLE comments (
                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          chapter_id UUID REFERENCES chapters(id) ON DELETE CASCADE,
                          user_id UUID REFERENCES users(id) ON DELETE CASCADE,
                          content TEXT NOT NULL,
                          created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE ratings (
                         id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                         series_id UUID REFERENCES series(id) ON DELETE CASCADE,
                         user_id UUID REFERENCES users(id) ON DELETE CASCADE,
                         rating INT CHECK (rating BETWEEN 1 AND 5),
                         created_at TIMESTAMP DEFAULT now(),
                         UNIQUE (series_id, user_id)
);
