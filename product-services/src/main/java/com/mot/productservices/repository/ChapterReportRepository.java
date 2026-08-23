package com.mot.productservices.repository;

import com.mot.productservices.entity.ChapterReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterReportRepository extends JpaRepository<ChapterReport, Long> {
    Page<ChapterReport> findAllByOrderByCreatedAtDesc(Pageable pageable);
    java.util.Optional<ChapterReport> findFirstByChapterIdAndUserIdAndReasonAndStatusInOrderByCreatedAtDesc(
            Integer chapterId, String userId, String reason, java.util.Collection<String> statuses);
}
