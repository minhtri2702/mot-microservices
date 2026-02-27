package com.mot.crawservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "crawled_chapters",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"series_id", "chapterNumber"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawledChapter {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", nullable = false)
    private CrawledSeries series;

    @Column(nullable = false)
    private Integer chapterNumber;

    private String title;

    @Column(name = "content_url")
    private String contentUrl;

    /**
     * 0 = chưa gửi Kafka
     * 1 = đã gửi
     * 2 = lỗi
     */
    @Column(name = "crawl_status")
    private Short crawlStatus = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
