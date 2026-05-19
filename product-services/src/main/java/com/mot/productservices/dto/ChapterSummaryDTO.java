package com.mot.productservices.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterSummaryDTO {
    private Integer id;
    private Double chapterNumber;
    private String chapterName;
    private Long viewCount;
    private String createdAt;
}
