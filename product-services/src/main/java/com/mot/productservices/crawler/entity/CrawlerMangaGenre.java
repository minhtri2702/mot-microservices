package com.mot.productservices.crawler.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "manga_genre")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlerMangaGenre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "manga_id", columnDefinition = "UUID", nullable = false)
    private String mangaId;

    @Column(name = "genre_id", nullable = false)
    private Integer genreId;
}
