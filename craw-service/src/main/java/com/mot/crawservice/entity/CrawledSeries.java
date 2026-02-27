package com.mot.crawservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "crawled_series")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawledSeries {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "source_url", nullable = false, unique = true)
    private String sourceUrl;

    @Column(nullable = false)
    private String title;

    private String author;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String coverUrl;

    /**
     * 0 = draft
     * 1 = ongoing
     * 2 = completed
     */
    private Short status;

    /**
     * 0 = chưa gửi Kafka
     * 1 = đã gửi
     * 2 = lỗi
     */
    @Column(name = "crawl_status")
    private Short crawlStatus = 0;

    private Integer retryCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
