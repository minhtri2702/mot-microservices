package com.mot.productservices.service;

import com.mot.productservices.dto.DataHealthIssueDTO;
import com.mot.productservices.dto.DataHealthSummaryDTO;
import com.mot.productservices.dto.PagedResponseDTO;
import com.mot.productservices.repository.ChapterRepository;
import com.mot.productservices.repository.MangaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DataHealthService {
    private final ChapterRepository chapterRepository;
    private final MangaRepository mangaRepository;

    public DataHealthSummaryDTO getSummary() {
        return DataHealthSummaryDTO.builder()
                .chaptersWithoutImages(chapterRepository.countChaptersWithoutImages())
                .imagesWithoutPath(chapterRepository.countImagesWithoutPath())
                .duplicatePageOrders(chapterRepository.countDuplicatePageOrders())
                .mangaWithoutCover(mangaRepository.countMissingCovers())
                .checkedAt(LocalDateTime.now())
                .build();
    }

    public PagedResponseDTO<DataHealthIssueDTO> getChaptersWithoutImages(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        Page<ChapterRepository.DataHealthIssueProjection> result =
                chapterRepository.findChaptersWithoutImages(PageRequest.of(safePage, safeSize));
        return PagedResponseDTO.<DataHealthIssueDTO>builder()
                .content(result.getContent().stream().map(item -> DataHealthIssueDTO.builder()
                        .chapterId(item.getChapterId())
                        .mangaId(item.getMangaId())
                        .mangaTitle(item.getMangaTitle())
                        .chapterNumber(item.getChapterNumber())
                        .chapterName(item.getChapterName())
                        .imageCount(item.getImageCount())
                        .issueType("MISSING_IMAGES")
                        .build()).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }
}
