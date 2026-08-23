package com.mot.productservices.repository;

import com.mot.productservices.entity.CrawlRepairJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface CrawlRepairJobRepository extends JpaRepository<CrawlRepairJob, UUID> {
    Optional<CrawlRepairJob> findFirstByChapterIdAndStatusInOrderByCreatedAtDesc(Integer chapterId, Collection<String> statuses);
}
