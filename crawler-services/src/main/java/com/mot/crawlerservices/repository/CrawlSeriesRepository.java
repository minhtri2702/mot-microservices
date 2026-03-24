package com.mot.crawlerservices.repository;

import com.mot.crawlerservices.entity.CrawlSeries;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CrawlSeriesRepository extends JpaRepository<CrawlSeries, UUID> {
    Optional<CrawlSeries> findBySourceAndExternalId(String source, String externalId);

}
