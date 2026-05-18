package com.mot.productservices.crawler.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chapter_image")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlerChapterImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "chapter_id", nullable = false)
    private Integer chapterId;

    @Column(name = "image_url", nullable = false, length = 2000)
    private String imageUrl;

    @Column(name = "image_path", length = 1000)
    private String imagePath;

    @Column(name = "page_order", nullable = false)
    private Integer pageOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
