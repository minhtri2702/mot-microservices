package com.mot.crawlerservices.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "crawl_chapters",
        uniqueConstraints = @UniqueConstraint(columnNames = {"series_external_id", "chapter_number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlChapter {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(name = "series_external_id", nullable = false)
    private String seriesExternalId;

    @Column(name = "chapter_number", nullable = false)
        private Integer chapterNumber;

    @Column(columnDefinition = "TEXT")
    private String url;

    @Column(length = 20)
    private String status; // new, done

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}