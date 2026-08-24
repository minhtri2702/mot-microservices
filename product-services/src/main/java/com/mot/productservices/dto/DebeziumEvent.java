package com.mot.productservices.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO để parse message từ Debezium CDC.
 * 
 * Debezium gửi message JSON dạng:
 * {
 *   "payload": {
 *     "before": null,
 *     "after": { "id": "uuid", "title": "...", ... },
 *     "source": { "table": "manga", "db": "crawler_db" },
 *     "op": "c",   // c=create, u=update, d=delete
 *     "ts_ms": 1234567890
 *   }
 * }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DebeziumEvent {

    private Payload payload;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Payload {
        private Before before;
        private After after;
        private Source source;
        private String op;          // c=create, u=update, d=delete
        private long ts_ms;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Before {
        private String id;          // manga UUID (dùng cho DELETE events)
        private Integer stt;
        private String title;
        private String url;
        // Chapter fields
        @JsonProperty("manga_id")
        private String mangaId;
        @JsonProperty("chapter_number")
        private Double chapterNumber;
        @JsonProperty("chapter_name")
        private String chapterName;
        // ChapterImage fields
        @JsonProperty("chapter_id")
        private Integer chapterId;
        @JsonProperty("page_order")
        private Integer pageOrder;
        @JsonProperty("image_url")
        private String imageUrl;
        @JsonProperty("image_path")
        private String imagePath;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class After {
        private String id;          // manga UUID
        private Integer stt;
        private String title;
        private String url;
        @JsonProperty("cover_image_path")
        private String coverImagePath;
        private String status;
        private String description;
        private String author;
        @JsonProperty("alternative_titles")
        private String alternativeTitles;
        @JsonProperty("created_date")
        private String createdDate;
        @JsonProperty("translation_team")
        private String translationTeam;
        @JsonProperty("age_rating")
        private String ageRating;
        private Long likes;
        private Long followers;
        private Long views;
        @JsonProperty("max_chapter_crawled")
        private Integer maxChapterCrawled;
        // Chapter fields
        @JsonProperty("manga_id")
        private String mangaId;
        @JsonProperty("chapter_number")
        private Double chapterNumber;
        @JsonProperty("chapter_name")
        private String chapterName;
        // ChapterImage fields
        @JsonProperty("chapter_id")
        private Integer chapterId;
        @JsonProperty("page_order")
        private Integer pageOrder;
        @JsonProperty("image_url")
        private String imageUrl;
        @JsonProperty("image_path")
        private String imagePath;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Source {
        private String table;       // "manga", "chapter", "chapter_image", "manga_genre"
        private String db;
        private long ts_ms;
    }

    public boolean isMangaTable() {
        return payload != null && payload.source != null && "manga".equals(payload.source.table);
    }

    public boolean isMangaGenreTable() {
        return payload != null && payload.source != null && "manga_genre".equals(payload.source.table);
    }

    public boolean isChapterTable() {
        return payload != null && payload.source != null && "chapter".equals(payload.source.table);
    }

    public boolean isChapterImageTable() {
        return payload != null && payload.source != null && "chapter_image".equals(payload.source.table);
    }

    public String getMangaId() {
        if (payload == null) return null;
        // CREATE/UPDATE: lấy từ 'after'
        if (payload.after != null) {
            return payload.after.getId();
        }
        // DELETE: lấy từ 'before' (vì 'after' = null)
        if (payload.before != null) {
            return payload.before.getId();
        }
        return null;
    }

    /**
     * Lấy manga_id từ event chapter/chapter_image.
     * Chapter/chapter_image có field manga_id riêng, khác với id của manga.
     */
    public String getMangaIdFromChapter() {
        if (payload == null) return null;
        if (payload.after != null) {
            return payload.after.getMangaId();
        }
        if (payload.before != null) {
            return payload.before.getMangaId();
        }
        return null;
    }

    /**
     * Lấy chapter_id từ event chapter_image.
     */
    public Integer getChapterId() {
        if (payload == null) return null;
        if (payload.after != null) {
            return payload.after.getChapterId();
        }
        if (payload.before != null) {
            return payload.before.getChapterId();
        }
        return null;
    }

    /**
     * Get the source primary key from a chapter event. Chapter IDs are kept
     * identical in crawler_db and mot_db so chapter_image.chapter_id remains
     * a valid reference after CDC synchronization.
     */
    public Integer getChapterRecordId() {
        if (payload == null) return null;
        String id = payload.after != null
                ? payload.after.getId()
                : payload.before != null ? payload.before.getId() : null;
        if (id == null || id.isBlank()) return null;
        return Integer.valueOf(id);
    }

    public String getOperation() {
        if (payload == null) return "UNKNOWN";
        return switch (payload.op) {
            case "c", "r" -> "CREATE";
            case "u" -> "UPDATE";
            case "d" -> "DELETE";
            default -> "UNKNOWN";
        };
    }
}
