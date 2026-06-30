package com.mot.productservices.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "manga")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Manga {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true)
    private Integer stt;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 1000, unique = true)
    private String url;

    @Column(name = "cover_image_path", length = 1000)
    private String coverImagePath;

    @Column(length = 50)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String author;

    @Column(name = "alternative_titles", columnDefinition = "TEXT")
    private String alternativeTitles;

    @Column(name = "created_date", length = 50)
    private String createdDate;

    @Column(name = "translation_team", length = 255)
    private String translationTeam;

    @Column(name = "age_rating", length = 50)
    private String ageRating;

    @Column(nullable = false)
    private Long likes = 0L;

    @Column(nullable = false)
    private Long followers = 0L;

    @Column(nullable = false)
    private Long views = 0L;

    @Column(name = "max_chapter_crawled")
    private Integer maxChapterCrawled = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "manga", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Chapter> chapters = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "manga_genre",
            joinColumns = @JoinColumn(name = "manga_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Genre> genres = new HashSet<>();
}
