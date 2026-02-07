


-- Tạo bảng series (truyện)
CREATE TABLE series (
                        series_id UUID  PRIMARY KEY,
                        seri_name VARCHAR(1000) NOT NULL,
                        status VARCHAR(50),
                        description TEXT,
                        cover_image_url VARCHAR(255),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)

-- Thêm ghi chú cho các cột trong bảng series
COMMENT ON COLUMN series.series_id IS 'Mã số duy nhất của series.';
COMMENT ON COLUMN series.seri_name IS 'Tiêu đề của series.';
COMMENT ON COLUMN series.status IS 'Trạng thái của series (ví dụ: đang tiến hành, hoàn thành).';
COMMENT ON COLUMN series.description IS 'Mô tả ngắn gọn về series.';
COMMENT ON COLUMN series.cover_image_url IS 'URL của ảnh bìa series.';
COMMENT ON COLUMN series.created_at IS 'Thời điểm series được tạo.';
COMMENT ON COLUMN series.updated_at IS 'Thời điểm series được cập nhật lần cuối.';

-- Tạo bảng chapters (chương truyện)
CREATE TABLE chapters (
                          chapter_id SERIAL PRIMARY KEY,
                          series_id uuid REFERENCES series(series_id),
                          chapter_number INTEGER,
                          title VARCHAR(255),
                          release_date DATE,
                          content_url VARCHAR(255),
                          view_count INTEGER DEFAULT 0,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          UNIQUE (series_id, chapter_number)
);

-- Thêm ghi chú cho các cột trong bảng chapters
COMMENT ON COLUMN chapters.chapter_id IS 'Mã số duy nhất của chương.';
COMMENT ON COLUMN chapters.series_id IS 'Mã số của series mà chương thuộc về.';
COMMENT ON COLUMN chapters.chapter_number IS 'Số thứ tự của chương trong series.';
COMMENT ON COLUMN chapters.title IS 'Tiêu đề của chương.';
COMMENT ON COLUMN chapters.release_date IS 'Ngày phát hành của chương.';
COMMENT ON COLUMN chapters.content_url IS 'URL hoặc đường dẫn đến nội dung chương.';
COMMENT ON COLUMN chapters.view_count IS 'Số lượt xem của chương.';
COMMENT ON COLUMN chapters.created_at IS 'Thời điểm chương được tạo.';
COMMENT ON COLUMN chapters.updated_at IS 'Thời điểm chương được cập nhật lần cuối.';


CREATE TABLE Image (
                       id SERIAL PRIMARY KEY,
                       chapter_id INT NOT null REFERENCES chapters(chapter_id),  -- Liên kết với bảng Chapter
                       series_id uuid not null ,
                       image_url VARCHAR(255) NOT NULL,  -- Đường dẫn tới hình ảnh
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       CONSTRAINT unique_chapter UNIQUE (chapter_id, series_id)
);


CREATE TABLE roles(
                      id INT PRIMARY KEY,
                      name VARCHAR(20) NOT NULL
);

CREATE TABLE users(
                      user_id uuid  PRIMARY KEY ,
                      username VARCHAR(100) DEFAULT '',
                      phone_number VARCHAR(10) NOT NULL,
                      address VARCHAR(200) DEFAULT '',
                      password VARCHAR(100) NOT NULL DEFAULT '',
                      created_at date,
                      updated_at Date,
                      is_active SMALLINT DEFAULT 1,
                      date_of_birth DATE,
                      facebook_account_id INT DEFAULT 0,
                      google_account_id INT DEFAULT 0 ,
                      role_id INT DEFAULT 0 references roles (id)
);
-- Thêm ghi chú cho các cột trong bảng users
COMMENT ON COLUMN users.user_id IS 'Mã số duy nhất của người dùng.';
COMMENT ON COLUMN users.username IS 'Tên đăng nhập duy nhất của người dùng.';
--COMMENT ON COLUMN users.password_hash IS 'Mật khẩu đã được băm của người dùng.';
COMMENT ON COLUMN users.google_account_id IS 'Địa chỉ email duy nhất của người dùng.';
COMMENT ON COLUMN users.facebook_account_id IS 'Địa chỉ facebook duy nhất của người dùng.';

--COMMENT ON COLUMN users.registration_date IS 'Thời điểm người dùng đăng ký.';
--COMMENT ON COLUMN users.last_login IS 'Thời điểm người dùng đăng nhập lần cuối.';


CREATE TABLE tokens(
                       id int PRIMARY KEY ,
                       token varchar(255) UNIQUE NOT NULL,
                       token_type varchar(50) NOT NULL,
                       expiration_date date,
                       revoked SMALLINT NOT NULL,
                       expired  smallint NOT NULL,
                       user_id uuid
                           REFERENCES users(user_id)
);
--hỗ trợ đăng nhập từ Facebook và Google
CREATE TABLE social_accounts(
                                id uuid PRIMARY KEY ,
                                provider VARCHAR(20) NOT NULL ,
                                provider_id VARCHAR(50) NOT NULL,
                                email VARCHAR(150) NOT NULL ,
                                name VARCHAR(100) NOT NULL,
                                user_id uuid,
                                FOREIGN KEY (user_id) REFERENCES users(user_id)
);



-- Tạo bảng user_chapter_status (trạng thái đọc của người dùng)
CREATE TABLE user_chapter_status (
                                     user_id uuid REFERENCES users(user_id),
                                     chapter_id INTEGER REFERENCES chapter(chapter_id),
                                     status VARCHAR(50),
                                     last_read_date TIMESTAMP,
                                     PRIMARY KEY (user_id, chapter_id)
);

-- Thêm ghi chú cho các cột trong bảng user_chapter_status
COMMENT ON COLUMN user_chapter_status.user_id IS 'Mã số của người dùng.';
COMMENT ON COLUMN user_chapter_status.chapter_id IS 'Mã số của chương.';
COMMENT ON COLUMN user_chapter_status.status IS 'Trạng thái đọc của chương (ví dụ: đã đọc, chưa đọc).';
COMMENT ON COLUMN user_chapter_status.last_read_date IS 'Thời điểm người dùng đọc chương lần cuối.';

-- Tạo bảng comments (bình luận)
CREATE TABLE comments (
                          comment_id SERIAL PRIMARY KEY,
                          chapter_id INTEGER REFERENCES chapters(chapter_id),
                          user_id uuid REFERENCES users(user_id),
                          seri_id uuid REFERENCES series(series_id),
                          comment_text TEXT NOT NULL,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Thêm ghi chú cho các cột trong bảng comments
COMMENT ON COLUMN comments.comment_id IS 'Mã số duy nhất của bình luận.';
COMMENT ON COLUMN comments.chapter_id IS 'Mã số của chương mà bình luận thuộc về.';
COMMENT ON COLUMN comments.user_id IS 'Mã số của người dùng đã viết bình luận.';
COMMENT ON COLUMN comments.comment_text IS 'Nội dung văn bản của bình luận.';
COMMENT ON COLUMN comments.created_at IS 'Thời điểm bình luận được tạo.';

-- Tạo bảng ratings (đánh giá)
CREATE TABLE ratings (
                         rating_id SERIAL PRIMARY KEY,
                         series_id uuid REFERENCES series(series_id),
    --user_id INTEGER REFERENCES users(id),
                         rating INTEGER CHECK (rating >= 1 AND rating <= 5),
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         UNIQUE (series_id, )
);

-- Thêm ghi chú cho các cột trong bảng ratings
COMMENT ON COLUMN ratings.rating_id IS 'Mã số duy nhất của đánh giá.';
COMMENT ON COLUMN ratings.series_id IS 'Mã số của series được đánh giá.';
COMMENT ON COLUMN ratings.user_id IS 'Mã số của người dùng đã đánh giá.';
COMMENT ON COLUMN ratings.rating IS 'Điểm đánh giá từ 1 đến 5.';
COMMENT ON COLUMN ratings.created_at IS 'Thời điểm đánh giá được tạo.';

-- Tạo bảng tags (thẻ)
CREATE TABLE tags (
                      tag_id int PRIMARY KEY,
                      tag_name VARCHAR(50) UNIQUE NOT NULL
);

-- Thêm ghi chú cho các cột trong bảng tags
COMMENT ON COLUMN tags.tag_id IS 'Mã số duy nhất của thẻ.';
COMMENT ON COLUMN tags.tag_name IS 'Tên duy nhất của thẻ (ví dụ: isekai, hài hước).';

-- Tạo bảng series_tags (mối quan hệ giữa truyện và thẻ)
CREATE TABLE series_tags (
                             series_id uuid REFERENCES series(series_id),
                             tag_id INTEGER REFERENCES tags(tag_id),
                             PRIMARY KEY (series_id, tag_id)
);

-- Thêm ghi chú cho các cột trong bảng series_tags
COMMENT ON COLUMN series_tags.series_id IS 'Mã số của series.';
COMMENT ON COLUMN series_tags.tag_id IS 'Mã số của thẻ liên kết với series.';

-- Tạo bảng genres (thể loại)
CREATE TABLE genres (
                        genre_id SERIAL PRIMARY KEY,
                        genre_name VARCHAR(50) UNIQUE NOT NULL
);

-- Thêm ghi chú cho các cột trong bảng genres
COMMENT ON COLUMN genres.genre_id IS 'Mã số duy nhất của thể loại.';
COMMENT ON COLUMN genres.genre_name IS 'Tên duy nhất của thể loại (ví dụ: Action, Romance).';

-- Tạo bảng series_genres (mối quan hệ giữa truyện và thể loại)
CREATE TABLE series_genres (
                               series_id uuid REFERENCES series(series_id),
                               genre_id INTEGER REFERENCES genres(genre_id),
                               PRIMARY KEY (series_id, genre_id)
);

-- Thêm ghi chú cho các cột trong bảng series_genres
COMMENT ON COLUMN series_genres.series_id IS 'Mã số của series.';
COMMENT ON COLUMN series_genres.genre_id IS 'Mã số của thể loại liên kết với series.';

-- Tạo bảng authors (tác giả)
CREATE TABLE authors (
                         author_id SERIAL PRIMARY KEY,
                         author_name VARCHAR(255) NOT NULL
);

-- Thêm ghi chú cho các cột trong bảng authors
COMMENT ON COLUMN authors.author_id IS 'Mã số duy nhất của tác giả.';
COMMENT ON COLUMN authors.author_name IS 'Tên của tác giả.';

-- Tạo bảng series_authors (mối quan hệ giữa truyện và tác giả)
CREATE TABLE series_authors (
                                series_id uuid REFERENCES series(series_id),
                                author_id INTEGER REFERENCES authors(author_id)

);

-- Thêm ghi chú cho các cột trong bảng series_authors
COMMENT ON COLUMN series_authors.series_id IS 'Mã số của series.';
COMMENT ON COLUMN series_authors.author_id IS 'Mã số của tác giả liên kết với series.';

-- Tạo bảng user_follows (người dùng theo dõi truyện)
CREATE TABLE user_follows (
                              user_id uuid REFERENCES users(user_id),
                              series_id uuid REFERENCES series(series_id),
                              followed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              PRIMARY KEY (user_id, series_id)
);

-- Thêm ghi chú cho các cột trong bảng user_follows
COMMENT ON COLUMN user_follows.user_id IS 'Mã số của người dùng.';
COMMENT ON COLUMN user_follows.series_id IS 'Mã số của series được theo dõi.';
COMMENT ON COLUMN user_follows.followed_at IS 'Thời điểm người dùng bắt đầu theo dõi series.';


CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
SELECT uuid_in(md5(random()::text || random()::text)::cstring);
