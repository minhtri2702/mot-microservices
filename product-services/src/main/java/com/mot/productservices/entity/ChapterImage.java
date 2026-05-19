package com.mot.productservices.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chapter_image")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChapterImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Chapter chapter;

    @Column(name = "image_url", length = 2000, nullable = false)
    private String imageUrl;

    @Column(name = "image_path", length = 1000)
    private String imagePath;

    @Column(name = "page_order", nullable = false)
    private Integer pageOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
