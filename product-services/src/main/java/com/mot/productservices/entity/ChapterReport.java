package com.mot.productservices.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chapter_report", indexes = {
        @Index(name = "idx_chapter_report_status_created", columnList = "status,created_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChapterReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "chapter_id", nullable = false)
    private Integer chapterId;
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;
    @Column(nullable = false, length = 40)
    private String reason;
    @Column(name = "page_index")
    private Integer pageIndex;
    @Column(length = 1000)
    private String details;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @PrePersist void create() { createdAt = updatedAt = LocalDateTime.now(); if (status == null) status = "OPEN"; }
    @PreUpdate void update() { updatedAt = LocalDateTime.now(); }
}
