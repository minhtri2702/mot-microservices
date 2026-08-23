package com.mot.productservices.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
public class CrawlRepairJobDTO {
    private UUID id;
    private Integer chapterId;
    private Long reportId;
    private String status;
    private String errorMessage;
    private Integer imagesDownloaded;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
