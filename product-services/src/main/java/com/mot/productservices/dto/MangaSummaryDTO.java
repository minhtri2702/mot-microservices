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
public class MangaSummaryDTO {
    private String id;
    private Integer stt;
    private String title;
    private String coverImagePath;
    private String status;
    private String author;
    private String description;
    private Long views;
    private Long likes;
    private Long followers;
    private Double latestChapter;
    private String latestChapterUpdatedAt;
    private List<String> genres;
}
