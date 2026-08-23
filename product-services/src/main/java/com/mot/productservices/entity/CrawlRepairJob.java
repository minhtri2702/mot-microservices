package com.mot.productservices.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "crawl_repair_job", indexes = {
        @Index(name = "idx_crawl_repair_job_status_created", columnList = "status,created_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CrawlRepairJob {
    @Id
    private UUID id;
    @Column(name = "chapter_id", nullable = false)
    private Integer chapterId;
    @Column(name = "report_id")
    private Long reportId;
    @Column(name = "requested_by", nullable = false, length = 64)
    private String requestedBy;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    @Column(name = "images_downloaded")
    private Integer imagesDownloaded;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @PrePersist void create() { if (id == null) id = UUID.randomUUID(); createdAt = updatedAt = LocalDateTime.now(); if (status == null) status = "QUEUED"; }
    @PreUpdate void update() { updatedAt = LocalDateTime.now(); }
}
