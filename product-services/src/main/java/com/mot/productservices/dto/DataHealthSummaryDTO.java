package com.mot.productservices.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DataHealthSummaryDTO {
    private long chaptersWithoutImages;
    private long imagesWithoutPath;
    private long duplicatePageOrders;
    private long mangaWithoutCover;
    private LocalDateTime checkedAt;
}
