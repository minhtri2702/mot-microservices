package com.mot.productservices.controller;

import com.mot.productservices.dto.*;
import com.mot.productservices.service.MangaService;
import com.mot.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MangaController {

    private final MangaService mangaService;

    // ==================== Manga Listing ====================

    @GetMapping("/manga/featured")
    public ResponseEntity<BaseResponse<List<MangaSummaryDTO>>> getFeaturedManga() {
        List<MangaSummaryDTO> result = mangaService.getFeaturedManga();
        return ResponseEntity.ok(BaseResponse.<List<MangaSummaryDTO>>ok(result));
    }

    @GetMapping({"/manga/latest-updated", "/manga/latest"})
    public ResponseEntity<BaseResponse<PagedResponseDTO<MangaSummaryDTO>>> getLatestUpdated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponseDTO<MangaSummaryDTO> result = mangaService.getLatestUpdated(page, size);
        return ResponseEntity.ok(BaseResponse.<PagedResponseDTO<MangaSummaryDTO>>ok(result));
    }

    @GetMapping("/manga/hot")
    public ResponseEntity<BaseResponse<PagedResponseDTO<MangaSummaryDTO>>> getHotManga(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponseDTO<MangaSummaryDTO> result = mangaService.getHotManga(page, size);
        return ResponseEntity.ok(BaseResponse.<PagedResponseDTO<MangaSummaryDTO>>ok(result));
    }

    @GetMapping("/manga/new")
    public ResponseEntity<BaseResponse<PagedResponseDTO<MangaSummaryDTO>>> getNewManga(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponseDTO<MangaSummaryDTO> result = mangaService.getNewManga(page, size);
        return ResponseEntity.ok(BaseResponse.<PagedResponseDTO<MangaSummaryDTO>>ok(result));
    }

    @GetMapping("/manga/completed")
    public ResponseEntity<BaseResponse<PagedResponseDTO<MangaSummaryDTO>>> getCompletedManga(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponseDTO<MangaSummaryDTO> result = mangaService.getCompletedManga(page, size);
        return ResponseEntity.ok(BaseResponse.<PagedResponseDTO<MangaSummaryDTO>>ok(result));
    }

    // ==================== Manga Detail ====================

    @GetMapping("/manga/{id}")
    public ResponseEntity<BaseResponse<MangaDetailDTO>> getMangaDetail(@PathVariable UUID id) {
        // Lấy dữ liệu từ cache (manga info + chapters)
        MangaDetailDTO result = mangaService.getMangaDetail(id);
        // Increment view count realtime (không cache)
        mangaService.incrementMangaView(id);
        return ResponseEntity.ok(BaseResponse.<MangaDetailDTO>ok(result));
    }

    // ==================== Chapter ====================

    @GetMapping("/manga/{mangaId}/chapters/{chapterId}")
    public ResponseEntity<BaseResponse<ChapterDetailDTO>> getChapterDetail(
            @PathVariable UUID mangaId,
            @PathVariable Integer chapterId) {
        ChapterDetailDTO result = mangaService.getChapterDetail(mangaId, chapterId);
        return ResponseEntity.ok(BaseResponse.<ChapterDetailDTO>ok(result));
    }

    // ==================== Search ====================

    @GetMapping("/manga/search")
    public ResponseEntity<BaseResponse<PagedResponseDTO<MangaSummaryDTO>>> searchManga(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponseDTO<MangaSummaryDTO> result = mangaService.searchManga(keyword, page, size);
        return ResponseEntity.ok(BaseResponse.<PagedResponseDTO<MangaSummaryDTO>>ok(result));
    }

    // ==================== Genre ====================

    @GetMapping("/genres")
    public ResponseEntity<BaseResponse<List<GenreDTO>>> getAllGenres() {
        List<GenreDTO> result = mangaService.getAllGenres();
        return ResponseEntity.ok(BaseResponse.<List<GenreDTO>>ok(result));
    }

    @GetMapping("/manga/genre/{genreId}")
    public ResponseEntity<BaseResponse<PagedResponseDTO<MangaSummaryDTO>>> getMangaByGenre(
            @PathVariable Integer genreId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponseDTO<MangaSummaryDTO> result = mangaService.getMangaByGenre(genreId, page, size);
        return ResponseEntity.ok(BaseResponse.<PagedResponseDTO<MangaSummaryDTO>>ok(result));
    }

    // ==================== Related ====================

    @GetMapping("/manga/{id}/related")
    public ResponseEntity<BaseResponse<PagedResponseDTO<MangaSummaryDTO>>> getRelatedManga(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        PagedResponseDTO<MangaSummaryDTO> result = mangaService.getRelatedManga(id, page, size);
        return ResponseEntity.ok(BaseResponse.<PagedResponseDTO<MangaSummaryDTO>>ok(result));
    }

    // ==================== Reading History ====================

    @GetMapping("/user/{userId}/reading-history")
    public ResponseEntity<BaseResponse<List<ReadingHistoryDTO>>> getReadingHistory(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int limit) {
        List<ReadingHistoryDTO> result = mangaService.getReadingHistory(userId, limit);
        return ResponseEntity.ok(BaseResponse.<List<ReadingHistoryDTO>>ok(result));
    }

    // ==================== Favorites ====================

    @PostMapping("/user/{userId}/favorites/{mangaId}")
    public ResponseEntity<BaseResponse<Void>> addFavorite(
            @PathVariable String userId,
            @PathVariable UUID mangaId) {
        mangaService.addFavorite(userId, mangaId);
        return ResponseEntity.ok(BaseResponse.<Void>ok(null));
    }

    @DeleteMapping("/user/{userId}/favorites/{mangaId}")
    public ResponseEntity<BaseResponse<Void>> removeFavorite(
            @PathVariable String userId,
            @PathVariable UUID mangaId) {
        mangaService.removeFavorite(userId, mangaId);
        return ResponseEntity.ok(BaseResponse.<Void>ok(null));
    }

    @GetMapping("/user/{userId}/favorites/{mangaId}/check")
    public ResponseEntity<BaseResponse<Boolean>> isFavorite(
            @PathVariable String userId,
            @PathVariable UUID mangaId) {
        boolean result = mangaService.isFavorite(userId, mangaId);
        return ResponseEntity.ok(BaseResponse.<Boolean>ok(result));
    }

    @GetMapping("/user/{userId}/favorites")
    public ResponseEntity<BaseResponse<PagedResponseDTO<FavoriteDTO>>> getFavorites(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponseDTO<FavoriteDTO> result = mangaService.getFavorites(userId, page, size);
        return ResponseEntity.ok(BaseResponse.<PagedResponseDTO<FavoriteDTO>>ok(result));
    }
}
