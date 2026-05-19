package com.mot.productservices.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterNavigationDTO {
    private Integer prevChapterId;
    private Double prevChapterNumber;
    private Integer nextChapterId;
    private Double nextChapterNumber;
}
