package com.mot.productservices.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteDTO {
    private String mangaId;
    private Integer stt;
    private String title;
    private String coverImagePath;
    private String author;
    private String status;
    private Long views;
    private Long likes;
    private Long followers;
    private Double latestChapter;
    private String latestChapterUpdatedAt;
}
