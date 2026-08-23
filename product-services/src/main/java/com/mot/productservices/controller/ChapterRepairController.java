package com.mot.productservices.controller;

import com.mot.productservices.dto.*;
import com.mot.productservices.service.ChapterRepairService;
import com.mot.response.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ChapterRepairController {
    private final ChapterRepairService repairService;

    @PostMapping("/chapters/{chapterId}/reports")
    public ResponseEntity<BaseResponse<ChapterReportDTO>> report(
            @PathVariable Integer chapterId, @Valid @RequestBody ChapterReportRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(BaseResponse.<ChapterReportDTO>ok(
                repairService.createReport(chapterId, authentication.getName(), request)));
    }

    @GetMapping("/admin/chapter-reports")
    public ResponseEntity<BaseResponse<PagedResponseDTO<ChapterReportDTO>>> reports(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(BaseResponse.<PagedResponseDTO<ChapterReportDTO>>ok(repairService.getReports(page, size)));
    }

    @PostMapping("/admin/crawl-repairs/{chapterId}")
    public ResponseEntity<BaseResponse<CrawlRepairJobDTO>> repair(
            @PathVariable Integer chapterId, @RequestParam(required = false) Long reportId,
            Authentication authentication) {
        return ResponseEntity.ok(BaseResponse.<CrawlRepairJobDTO>ok(
                repairService.enqueue(chapterId, reportId, authentication.getName())));
    }

    @GetMapping("/admin/crawl-repairs/jobs/{jobId}")
    public ResponseEntity<BaseResponse<CrawlRepairJobDTO>> job(@PathVariable UUID jobId) {
        return ResponseEntity.ok(BaseResponse.<CrawlRepairJobDTO>ok(repairService.getJob(jobId)));
    }
}
