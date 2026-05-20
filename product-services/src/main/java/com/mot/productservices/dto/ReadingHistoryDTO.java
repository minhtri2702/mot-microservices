package com.mot.productservices.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingHistoryDTO {
    private String mangaId;
    private String mangaTitle;
    private String coverImagePath;
    private Integer stt;
    private Integer chapterId;
    private Double chapterNumber;
    private String chapterName;
    private String lastReadDate;
}
