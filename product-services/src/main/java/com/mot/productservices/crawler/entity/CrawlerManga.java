package com.mot.productservices.crawler.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "manga")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlerManga {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Column(name = "stt", unique = true, nullable = false)
    private Integer stt;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "url", length = 1000)
    private String url;

    @Column(name = "cover_image_path", length = 1000)
    private String coverImagePath;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "author", length = 255)
    private String author;

    @Column(name = "alternative_titles", columnDefinition = "TEXT")
    private String alternativeTitles;

    @Column(name = "created_date", length = 50)
    private String createdDate;

    @Column(name = "translation_team", length = 255)
    private String translationTeam;

    @Column(name = "age_rating", length = 50)
    private String ageRating;

    @Column(name = "likes")
    private Long likes;

    @Column(name = "followers")
    private Long followers;

    @Column(name = "views")
    private Long views;

    @Column(name = "max_chapter_crawled")
    private Integer maxChapterCrawled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
