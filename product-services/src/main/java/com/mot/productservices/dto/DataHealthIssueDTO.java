package com.mot.productservices.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataHealthIssueDTO {
    private Integer chapterId;
    private String mangaId;
    private String mangaTitle;
    private Double chapterNumber;
    private String chapterName;
    private Integer imageCount;
    private String issueType;
}
