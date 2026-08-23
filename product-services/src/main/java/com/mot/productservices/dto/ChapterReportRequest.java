package com.mot.productservices.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChapterReportRequest {
    @NotBlank @Size(max = 40)
    private String reason;
    @Min(0)
    private Integer pageIndex;
    @Size(max = 1000)
    private String details;
}
