package com.mot.productservices.controller;

import com.mot.productservices.dto.DataHealthIssueDTO;
import com.mot.productservices.dto.DataHealthSummaryDTO;
import com.mot.productservices.dto.PagedResponseDTO;
import com.mot.productservices.service.DataHealthService;
import com.mot.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/data-health")
@RequiredArgsConstructor
public class AdminDataHealthController {
    private final DataHealthService dataHealthService;

    @GetMapping("/summary")
    public ResponseEntity<BaseResponse<DataHealthSummaryDTO>> getSummary() {
        return ResponseEntity.ok(BaseResponse.<DataHealthSummaryDTO>ok(dataHealthService.getSummary()));
    }

    @GetMapping("/chapters-without-images")
    public ResponseEntity<BaseResponse<PagedResponseDTO<DataHealthIssueDTO>>> getChaptersWithoutImages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(BaseResponse.<PagedResponseDTO<DataHealthIssueDTO>>ok(
                dataHealthService.getChaptersWithoutImages(page, size)));
    }
}
