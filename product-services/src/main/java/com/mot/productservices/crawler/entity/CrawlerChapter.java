package com.mot.productservices.crawler.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chapter")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlerChapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "manga_id", columnDefinition = "UUID", nullable = false)
    private String mangaId;

    @Column(name = "chapter_number", nullable = false)
    private Double chapterNumber;

    @Column(name = "chapter_name", length = 500)
    private String chapterName;

    @Column(name = "url", length = 1000, unique = true)
    private String url;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
