package com.mot.productservices.crawler.repository;

import com.mot.productservices.crawler.entity.CrawlerChapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrawlerChapterRepository extends JpaRepository<CrawlerChapter, Integer> {
    List<CrawlerChapter> findByMangaId(String mangaId);
}
