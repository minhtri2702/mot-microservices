package com.mot.productservices.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
public class UserNotificationDTO {
    private Long id;
    private UUID mangaId;
    private Integer chapterId;
    private String type;
    private String title;
    private boolean read;
    private LocalDateTime createdAt;
}
