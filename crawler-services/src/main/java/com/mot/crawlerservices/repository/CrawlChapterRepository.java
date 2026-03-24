package com.mot.crawlerservices.repository;

import com.mot.crawlerservices.entity.CrawlChapter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CrawlChapterRepository extends JpaRepository<CrawlChapter, UUID> {
    Optional<CrawlChapter> findBySeriesExternalIdAndChapterNumber(String seriesExternalId, Integer chapterNumber);
}
