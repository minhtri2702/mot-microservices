package com.mot.productservices.controller;

import com.mot.productservices.dto.*;
import com.mot.productservices.service.MangaService;
import com.mot.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MangaController {

    private final MangaService mangaService;

    // ==================== Listing APIs ====================

    @GetMapping("/manga/featured")
    public ResponseEntity<BaseResponse<List<MangaSummaryDTO>>> getFeaturedManga() {
        List<MangaSummaryDTO> result = mangaService.getFeaturedManga();
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @GetMapping("/manga/latest-updated")
    public ResponseEntity<BaseResponse<PagedResponseDTO<MangaSummaryDTO>>> getLatestUpdates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(BaseResponse.success(mangaService.getLatestUpdates(page, size)));
    }

    @GetMapping("/manga/hot")
    public ResponseEntity<BaseResponse<PagedResponseDTO<MangaSummaryDTO>>> getHotManga(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(BaseResponse.success(mangaService.getHotManga(page, size)));
    }

    @GetMapping("/manga/new")
    public ResponseEntity<BaseResponse<PagedResponseDTO<MangaSummaryDTO>>> getNewManga(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(BaseResponse.success(mangaService.getNewManga(page, size)));
    }

    @GetMapping("/manga/completed")
    public ResponseEntity<BaseResponse<PagedResponseDTO<MangaSummaryDTO>>> getCompletedManga(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(BaseResponse.success(mangaService.getCompletedManga(page, size)));
    }

    // ==================== Detail API ====================

    @GetMapping("/manga/{id}")
    public ResponseEntity<BaseResponse<MangaDetailDTO>> getMangaDetail(@PathVariable String id) {
        return ResponseEntity.ok(BaseResponse.success(mangaService.getMangaDetail(id)));
    }

    // ==================== Chapter API ====================

    @GetMapping("/manga/{mangaId}/chapters/{chapterId}")
    public ResponseEntity<BaseResponse<ChapterDetailDTO>> getChapterDetail(
            @PathVariable String mangaId,
            @PathVariable Integer chapterId) {
        return ResponseEntity.ok(BaseResponse.success(mangaService.getChapterDetail(mangaId, chapterId)));
    }

    // ==================== Search API ====================

    @GetMapping("/manga/search")
    public ResponseEntity<BaseResponse<PagedResponseDTO<MangaSummaryDTO>>> searchManga(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(BaseResponse.success(mangaService.searchManga(keyword, page, size)));
    }

    // ==================== Genre APIs ====================

    @GetMapping("/genres")
    public ResponseEntity<BaseResponse<List<GenreDTO>>> getAllGenres() {
        return ResponseEntity.ok(BaseResponse.success(mangaService.getAllGenres()));
    }

    @GetMapping("/manga/genre/{genreId}")
    public ResponseEntity<BaseResponse<PagedResponseDTO<MangaSummaryDTO>>> getMangaByGenre(
            @PathVariable Integer genreId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(BaseResponse.success(mangaService.getMangaByGenre(genreId, page, size)));
    }

    // ==================== Related Manga API ====================

    @GetMapping("/manga/{id}/related")
    public ResponseEntity<BaseResponse<PagedResponseDTO<MangaSummaryDTO>>> getRelatedManga(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(BaseResponse.success(mangaService.getRelatedManga(id, page, size)));
    }
}
