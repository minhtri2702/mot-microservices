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
public class ChapterDetailDTO {
    private Integer id;
    private Double chapterNumber;
    private String chapterName;
    private Long viewCount;
    private String createdAt;
    private List<String> imageUrls;
    private ChapterNavigationDTO navigation;
}
