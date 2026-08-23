package com.mot.productservices.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_notification", uniqueConstraints =
        @UniqueConstraint(columnNames = {"user_id", "chapter_id", "type"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserNotification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;
    @Column(name = "manga_id", nullable = false)
    private UUID mangaId;
    @Column(name = "chapter_id", nullable = false)
    private Integer chapterId;
    @Column(nullable = false, length = 40)
    private String type;
    @Column(nullable = false, length = 500)
    private String title;
    @Column(name = "read_at")
    private LocalDateTime readAt;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
