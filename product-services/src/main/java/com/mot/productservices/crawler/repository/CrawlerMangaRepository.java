package com.mot.productservices.crawler.repository;

import com.mot.productservices.crawler.entity.CrawlerManga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CrawlerMangaRepository extends JpaRepository<CrawlerManga, UUID> {
}
