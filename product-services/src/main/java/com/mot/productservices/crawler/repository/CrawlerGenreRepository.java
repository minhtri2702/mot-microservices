package com.mot.productservices.crawler.repository;

import com.mot.productservices.crawler.entity.CrawlerGenre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CrawlerGenreRepository extends JpaRepository<CrawlerGenre, Integer> {
}
