package com.mot.productservices.crawler.repository;

import com.mot.productservices.crawler.entity.CrawlerChapterImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrawlerChapterImageRepository extends JpaRepository<CrawlerChapterImage, Integer> {
    List<CrawlerChapterImage> findByChapterId(Integer chapterId);
}
