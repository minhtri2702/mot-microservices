package com.mot.productservices.crawler.repository;

import com.mot.productservices.crawler.entity.CrawlerMangaGenre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrawlerMangaGenreRepository extends JpaRepository<CrawlerMangaGenre, Long> {
    List<CrawlerMangaGenre> findByMangaId(String mangaId);
}
