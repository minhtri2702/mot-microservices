package com.mot.productservices.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class ChapterReportDTO {
    private Long id;
    private Integer chapterId;
    private String userId;
    private String reason;
    private Integer pageIndex;
    private String details;
    private String status;
    private LocalDateTime createdAt;
}
