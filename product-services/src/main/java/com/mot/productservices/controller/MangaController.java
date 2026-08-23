package com.mot.productservices.controller;

import com.mot.productservices.dto.*;
import com.mot.productservices.service.MangaService;
import com.mot.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
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
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        String authenticatedUserId = requireMatchingUser(userId, authentication);
        List<ReadingHistoryDTO> result = mangaService.getReadingHistory(authenticatedUserId, limit);
        return ResponseEntity.ok(BaseResponse.<List<ReadingHistoryDTO>>ok(result));
    }

    // ==================== Favorites ====================

    @PostMapping("/user/{userId}/favorites/{mangaId}")
    public ResponseEntity<BaseResponse<Void>> addFavorite(
            @PathVariable String userId,
            @PathVariable UUID mangaId,
            Authentication authentication) {
        mangaService.addFavorite(requireMatchingUser(userId, authentication), mangaId);
        return ResponseEntity.ok(BaseResponse.<Void>ok());
    }

    @DeleteMapping("/user/{userId}/favorites/{mangaId}")
    public ResponseEntity<BaseResponse<Void>> removeFavorite(
            @PathVariable String userId,
            @PathVariable UUID mangaId,
            Authentication authentication) {
        mangaService.removeFavorite(requireMatchingUser(userId, authentication), mangaId);
        return ResponseEntity.ok(BaseResponse.<Void>ok());
    }

    @GetMapping("/user/{userId}/favorites/{mangaId}/check")
    public ResponseEntity<BaseResponse<Boolean>> isFavorite(
            @PathVariable String userId,
            @PathVariable UUID mangaId,
            Authentication authentication) {
        boolean result = mangaService.isFavorite(requireMatchingUser(userId, authentication), mangaId);
        return ResponseEntity.ok(BaseResponse.<Boolean>ok(result));
    }

    @GetMapping("/user/{userId}/favorites")
    public ResponseEntity<BaseResponse<PagedResponseDTO<FavoriteDTO>>> getFavorites(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        PagedResponseDTO<FavoriteDTO> result = mangaService.getFavorites(
                requireMatchingUser(userId, authentication), page, size);
        return ResponseEntity.ok(BaseResponse.<PagedResponseDTO<FavoriteDTO>>ok(result));
    }

    // ==================== Comments ====================

    @GetMapping("/manga/{mangaId}/comments")
    public ResponseEntity<BaseResponse<PagedResponseDTO<CommentDTO>>> getComments(
            @PathVariable UUID mangaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String currentUserId = isAuthenticated(authentication) ? authentication.getName() : null;
        PagedResponseDTO<CommentDTO> result = mangaService.getComments(mangaId, page, size, currentUserId);
        return ResponseEntity.ok(BaseResponse.<PagedResponseDTO<CommentDTO>>ok(result));
    }

    @PostMapping("/manga/{mangaId}/comments")
    public ResponseEntity<BaseResponse<CommentDTO>> addComment(
            @PathVariable UUID mangaId,
            @RequestBody CommentRequest request,
            Authentication authentication) {
        String userId = requireAuthenticatedUser(authentication);
        String username = authentication.getDetails() instanceof String
                ? (String) authentication.getDetails() : userId;
        CommentDTO result = mangaService.addComment(mangaId, userId, username, null, request);
        return ResponseEntity.ok(BaseResponse.<CommentDTO>ok(result));
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<BaseResponse<CommentDTO>> updateComment(
            @PathVariable UUID commentId,
            @RequestBody CommentRequest request,
            Authentication authentication) {
        CommentDTO result = mangaService.updateComment(
                commentId, requireAuthenticatedUser(authentication), request.getContent());
        return ResponseEntity.ok(BaseResponse.<CommentDTO>ok(result));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<BaseResponse<Void>> deleteComment(
            @PathVariable UUID commentId,
            Authentication authentication) {
        mangaService.deleteComment(commentId, requireAuthenticatedUser(authentication));
        return ResponseEntity.ok(BaseResponse.<Void>ok());
    }

    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<BaseResponse<Void>> toggleLikeComment(
            @PathVariable UUID commentId,
            Authentication authentication) {
        mangaService.toggleLikeComment(commentId, requireAuthenticatedUser(authentication));
        return ResponseEntity.ok(BaseResponse.<Void>ok());
    }

    // ==================== User Comments ====================

    @GetMapping("/user/{userId}/comments")
    public ResponseEntity<BaseResponse<PagedResponseDTO<CommentDTO>>> getUserComments(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        PagedResponseDTO<CommentDTO> result = mangaService.getUserComments(
                requireMatchingUser(userId, authentication), page, size);
        return ResponseEntity.ok(BaseResponse.<PagedResponseDTO<CommentDTO>>ok(result));
    }

    private String requireAuthenticatedUser(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            throw new AccessDeniedException("Authentication is required");
        }
        return authentication.getName();
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private String requireMatchingUser(String requestedUserId, Authentication authentication) {
        String authenticatedUserId = requireAuthenticatedUser(authentication);
        if (!authenticatedUserId.equals(requestedUserId)) {
            throw new AccessDeniedException("Cannot access another user's data");
        }
        return authenticatedUserId;
    }
}
