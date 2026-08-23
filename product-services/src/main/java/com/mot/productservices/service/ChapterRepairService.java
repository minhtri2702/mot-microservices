package com.mot.productservices.service;

import com.mot.productservices.dto.*;
import com.mot.productservices.entity.ChapterReport;
import com.mot.productservices.entity.CrawlRepairJob;
import com.mot.productservices.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ChapterRepairService {
    private final ChapterRepository chapterRepository;
    private final ChapterReportRepository reportRepository;
    private final CrawlRepairJobRepository jobRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${mot.crawl.repair-request-topic:mot.crawl.repair.request}")
    private String requestTopic;

    @Transactional
    public ChapterReportDTO createReport(Integer chapterId, String userId, ChapterReportRequest request) {
        if (!chapterRepository.existsById(chapterId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found");
        String reason = request.getReason().trim().toUpperCase();
        if (!List.of("MISSING_IMAGE", "WRONG_ORDER", "DUPLICATE_IMAGE", "BLURRY_IMAGE", "WRONG_CHAPTER", "OTHER").contains(reason)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported report reason");
        }
        var existing = reportRepository.findFirstByChapterIdAndUserIdAndReasonAndStatusInOrderByCreatedAtDesc(
                chapterId, userId, reason, List.of("OPEN", "QUEUED", "RUNNING"));
        if (existing.isPresent()) return toReportDTO(existing.get());
        String details = request.getDetails() == null ? null : request.getDetails().trim();
        ChapterReport report = reportRepository.save(ChapterReport.builder()
                .chapterId(chapterId).userId(userId).reason(reason)
                .pageIndex(request.getPageIndex()).details(details == null || details.isEmpty() ? null : details)
                .status("OPEN").build());
        return toReportDTO(report);
    }

    @Transactional(readOnly = true)
    public PagedResponseDTO<ChapterReportDTO> getReports(int page, int size) {
        Page<ChapterReport> result = reportRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size))));
        return PagedResponseDTO.<ChapterReportDTO>builder()
                .content(result.getContent().stream().map(this::toReportDTO).toList())
                .page(result.getNumber()).size(result.getSize()).totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages()).first(result.isFirst()).last(result.isLast()).build();
    }

    public CrawlRepairJobDTO enqueue(Integer chapterId, Long reportId, String requestedBy) {
        if (!chapterRepository.existsById(chapterId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found");
        return jobRepository.findFirstByChapterIdAndStatusInOrderByCreatedAtDesc(chapterId, List.of("QUEUED", "RUNNING"))
                .map(this::toJobDTO)
                .orElseGet(() -> createAndPublish(chapterId, reportId, requestedBy));
    }

    private CrawlRepairJobDTO createAndPublish(Integer chapterId, Long reportId, String requestedBy) {
        if (reportId != null) {
            ChapterReport report = reportRepository.findById(reportId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter report not found"));
            if (!chapterId.equals(report.getChapterId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Report does not belong to this chapter");
            }
        }
        CrawlRepairJob job = jobRepository.save(CrawlRepairJob.builder()
                .id(UUID.randomUUID()).chapterId(chapterId).reportId(reportId)
                .requestedBy(requestedBy).status("QUEUED").build());
        boolean force = reportId != null && reportRepository.findById(reportId)
                .map(report -> !"MISSING_IMAGE".equals(report.getReason()))
                .orElse(false);
        try {
            kafkaTemplate.send(requestTopic, chapterId.toString(), Map.of(
                    "jobId", job.getId().toString(), "chapterId", chapterId, "force", force))
                    .get(10, TimeUnit.SECONDS);
        } catch (Exception exception) {
            job.setStatus("FAILED");
            job.setErrorMessage("Kafka unavailable: " + exception.getClass().getSimpleName());
            jobRepository.save(job);
        }
        if (reportId != null) reportRepository.findById(reportId).ifPresent(report -> {
            report.setStatus("FAILED".equals(job.getStatus()) ? "OPEN" : "QUEUED");
            reportRepository.save(report);
        });
        return toJobDTO(job);
    }

    @Transactional(readOnly = true)
    public CrawlRepairJobDTO getJob(UUID jobId) {
        return jobRepository.findById(jobId).map(this::toJobDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repair job not found"));
    }

    @Transactional
    public void applyResult(Map<String, Object> result) {
        UUID jobId = UUID.fromString(String.valueOf(result.get("jobId")));
        jobRepository.findById(jobId).ifPresent(job -> {
            String status = String.valueOf(result.getOrDefault("status", "FAILED"));
            if (!List.of("RUNNING", "SUCCEEDED", "FAILED").contains(status)) {
                throw new IllegalArgumentException("Unsupported repair status: " + status);
            }
            job.setStatus(status);
            job.setImagesDownloaded(number(result.get("imagesDownloaded")));
            Object error = result.get("error");
            job.setErrorMessage(error == null ? null : String.valueOf(error).substring(0, Math.min(1000, String.valueOf(error).length())));
            jobRepository.save(job);
            if (job.getReportId() != null) reportRepository.findById(job.getReportId()).ifPresent(report -> {
                report.setStatus("SUCCEEDED".equals(status) ? "RESOLVED" :
                        "FAILED".equals(status) ? "OPEN" : status);
                reportRepository.save(report);
            });
        });
    }

    private Integer number(Object value) { return value instanceof Number number ? number.intValue() : 0; }
    private ChapterReportDTO toReportDTO(ChapterReport item) { return ChapterReportDTO.builder().id(item.getId()).chapterId(item.getChapterId()).userId(item.getUserId()).reason(item.getReason()).pageIndex(item.getPageIndex()).details(item.getDetails()).status(item.getStatus()).createdAt(item.getCreatedAt()).build(); }
    private CrawlRepairJobDTO toJobDTO(CrawlRepairJob item) { return CrawlRepairJobDTO.builder().id(item.getId()).chapterId(item.getChapterId()).reportId(item.getReportId()).status(item.getStatus()).errorMessage(item.getErrorMessage()).imagesDownloaded(item.getImagesDownloaded()).createdAt(item.getCreatedAt()).updatedAt(item.getUpdatedAt()).build(); }
}
