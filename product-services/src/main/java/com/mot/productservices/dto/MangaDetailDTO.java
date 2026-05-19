package com.mot.productservices.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MangaDetailDTO {
    private String id;
    private Integer stt;
    private String title;
    private String coverImagePath;
    private String status;
    private String description;
    private String author;
    private String alternativeTitles;
    private String createdDate;
    private String translationTeam;
    private String ageRating;
    private Long likes;
    private Long followers;
    private Long views;
    private Long realViews;
    private Double latestChapter;
    private String latestChapterUpdatedAt;
    private List<String> genres;
    private List<ChapterSummaryDTO> chapters;
}
