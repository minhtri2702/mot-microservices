package com.mot.productservices.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_chapter_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(UserChapterStatusId.class)
public class UserChapterStatus {

    @Id
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Id
    @Column(name = "chapter_id", nullable = false)
    private Integer chapterId;

    @Column(length = 50)
    private String status;

    @Column(name = "last_read_date")
    private LocalDateTime lastReadDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", insertable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Chapter chapter;
}
