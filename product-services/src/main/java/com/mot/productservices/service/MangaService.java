package com.mot.productservices.service;

import com.mot.productservices.dto.*;
import com.mot.productservices.entity.Chapter;
import com.mot.productservices.entity.ChapterImage;
import com.mot.productservices.entity.Genre;
import com.mot.productservices.entity.Manga;
import com.mot.productservices.repository.ChapterRepository;
import com.mot.productservices.repository.GenreRepository;
import com.mot.productservices.repository.MangaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MangaService {

    private final MangaRepository mangaRepository;
    private final ChapterRepository chapterRepository;
    private final GenreRepository genreRepository;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // ==================== Mapping helpers ====================

    private MangaSummaryDTO toSummaryDTO(Manga manga) {
        Double latestChapter = null;
        String latestChapterUpdatedAt = null;
        if (manga.getChapters() != null && !manga.getChapters().isEmpty()) {
            Chapter last = manga.getChapters().get(manga.getChapters().size() - 1);
            latestChapter = last.getChapterNumber();
            latestChapterUpdatedAt = last.getCreatedAt() != null ? last.getCreatedAt().format(DTF) : null;
        }

        List<String> genreNames = manga.getGenres() != null
                ? manga.getGenres().stream().map(Genre::getName).collect(Collectors.toList())
                : Collections.emptyList();

        return MangaSummaryDTO.builder()
                .id(manga.getId().toString())
                .stt(manga.getStt())
                .title(manga.getTitle())
                .coverImagePath(manga.getCoverImagePath())
                .status(manga.getStatus())
                .author(manga.getAuthor())
                .views(manga.getViews())
                .likes(manga.getLikes())
                .followers(manga.getFollowers())
                .latestChapter(latestChapter)
                .latestChapterUpdatedAt(latestChapterUpdatedAt)
                .genres(genreNames)
                .build();
    }

    private MangaDetailDTO toDetailDTO(Manga manga) {
        Double latestChapter = null;
        String latestChapterUpdatedAt = null;
        if (manga.getChapters() != null && !manga.getChapters().isEmpty()) {
            Chapter last = manga.getChapters().get(manga.getChapters().size() - 1);
            latestChapter = last.getChapterNumber();
            latestChapterUpdatedAt = last.getCreatedAt() != null ? last.getCreatedAt().format(DTF) : null;
        }

        List<String> genreNames = manga.getGenres() != null
                ? manga.getGenres().stream().map(Genre::getName).collect(Collectors.toList())
                : Collections.emptyList();

        List<ChapterSummaryDTO> chapterDTOs = manga.getChapters() != null
                ? manga.getChapters().stream().map(this::toChapterSummaryDTO).collect(Collectors.toList())
                : Collections.emptyList();

        return MangaDetailDTO.builder()
                .id(manga.getId().toString())
                .stt(manga.getStt())
                .title(manga.getTitle())
                .coverImagePath(manga.getCoverImagePath())
                .status(manga.getStatus())
                .description(manga.getDescription())
                .author(manga.getAuthor())
                .alternativeTitles(manga.getAlternativeTitles())
                .createdDate(manga.getCreatedDate())
                .translationTeam(manga.getTranslationTeam())
                .ageRating(manga.getAgeRating())
                .likes(manga.getLikes())
                .followers(manga.getFollowers())
                .views(manga.getViews())
                .realViews(manga.getViews())
                .latestChapter(latestChapter)
                .latestChapterUpdatedAt(latestChapterUpdatedAt)
                .genres(genreNames)
                .chapters(chapterDTOs)
                .build();
    }

    private ChapterSummaryDTO toChapterSummaryDTO(Chapter chapter) {
        return ChapterSummaryDTO.builder()
                .id(chapter.getId())
                .chapterNumber(chapter.getChapterNumber())
                .chapterName(chapter.getChapterName())
                .viewCount(0L) // Default, can be updated later
                .createdAt(chapter.getCreatedAt() != null ? chapter.getCreatedAt().format(DTF) : null)
                .build();
    }

    // ==================== Business methods ====================

    public List<MangaSummaryDTO> getFeaturedManga() {
        return mangaRepository.findTop10ByOrderByViewsDesc()
                .stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    public PagedResponseDTO<MangaSummaryDTO> getLatestUpdates(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Manga> mangaPage = mangaRepository.findAllByOrderByUpdatedAtDesc(pageable);
        List<MangaSummaryDTO> content = mangaPage.getContent().stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
        return PagedResponseDTO.from(mangaPage, content);
    }

    public PagedResponseDTO<MangaSummaryDTO> getHotManga(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Manga> mangaPage = mangaRepository.findAllByOrderByViewsDesc(pageable);
        List<MangaSummaryDTO> content = mangaPage.getContent().stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
        return PagedResponseDTO.from(mangaPage, content);
    }

    public PagedResponseDTO<MangaSummaryDTO> getNewManga(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Manga> mangaPage = mangaRepository.findAllByOrderByCreatedAtDesc(pageable);
        List<MangaSummaryDTO> content = mangaPage.getContent().stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
        return PagedResponseDTO.from(mangaPage, content);
    }

    public PagedResponseDTO<MangaSummaryDTO> getCompletedManga(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Manga> mangaPage = mangaRepository.findByStatusContainingIgnoreCaseOrderByUpdatedAtDesc("hoàn thành", pageable);
        List<MangaSummaryDTO> content = mangaPage.getContent().stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
        return PagedResponseDTO.from(mangaPage, content);
    }

    public MangaDetailDTO getMangaDetail(String id) {
        UUID uuid = UUID.fromString(id);
        Manga manga = mangaRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("Manga not found: " + id));
        return toDetailDTO(manga);
    }

    public ChapterDetailDTO getChapterDetail(String mangaId, Integer chapterId) {
        UUID uuid = UUID.fromString(mangaId);
        Chapter chapter = chapterRepository.findByIdAndMangaId(chapterId, uuid)
                .orElseThrow(() -> new RuntimeException("Chapter not found: " + chapterId));

        List<String> imageUrls = chapter.getImages() != null
                ? chapter.getImages().stream()
                .map(ChapterImage::getImageUrl)
                .collect(Collectors.toList())
                : Collections.emptyList();

        // Find prev/next chapter
        ChapterNavigationDTO navigation = buildNavigation(uuid, chapter.getChapterNumber());

        return ChapterDetailDTO.builder()
                .id(chapter.getId())
                .chapterNumber(chapter.getChapterNumber())
                .chapterName(chapter.getChapterName())
                .viewCount(0L)
                .createdAt(chapter.getCreatedAt() != null ? chapter.getCreatedAt().format(DTF) : null)
                .imageUrls(imageUrls)
                .navigation(navigation)
                .build();
    }

    private ChapterNavigationDTO buildNavigation(UUID mangaId, Double currentChapterNumber) {
        Optional<Chapter> prev = chapterRepository.findPreviousChapter(mangaId, currentChapterNumber);
        Optional<Chapter> next = chapterRepository.findNextChapter(mangaId, currentChapterNumber);

        return ChapterNavigationDTO.builder()
                .prevChapterId(prev.map(Chapter::getId).orElse(null))
                .prevChapterNumber(prev.map(Chapter::getChapterNumber).orElse(null))
                .nextChapterId(next.map(Chapter::getId).orElse(null))
                .nextChapterNumber(next.map(Chapter::getChapterNumber).orElse(null))
                .build();
    }

    public PagedResponseDTO<MangaSummaryDTO> searchManga(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Manga> mangaPage = mangaRepository.searchByTitle(keyword, pageable);
        List<MangaSummaryDTO> content = mangaPage.getContent().stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
        return PagedResponseDTO.from(mangaPage, content);
    }

    public List<GenreDTO> getAllGenres() {
        return genreRepository.findAll().stream()
                .map(g -> GenreDTO.builder()
                        .id(g.getId())
                        .name(g.getName())
                        .slug(g.getSlug())
                        .build())
                .collect(Collectors.toList());
    }

    public PagedResponseDTO<MangaSummaryDTO> getMangaByGenre(Integer genreId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Manga> mangaPage = mangaRepository.findByGenreId(genreId, pageable);
        List<MangaSummaryDTO> content = mangaPage.getContent().stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
        return PagedResponseDTO.from(mangaPage, content);
    }

    public PagedResponseDTO<MangaSummaryDTO> getRelatedManga(String id, int page, int size) {
        UUID uuid = UUID.fromString(id);
        Manga manga = mangaRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("Manga not found: " + id));

        List<Integer> genreIds = manga.getGenres().stream()
                .map(Genre::getId)
                .collect(Collectors.toList());

        if (genreIds.isEmpty()) {
            return PagedResponseDTO.<MangaSummaryDTO>builder()
                    .content(Collections.emptyList())
                    .page(page)
                    .size(size)
                    .totalElements(0)
                    .totalPages(0)
                    .last(true)
                    .first(true)
                    .build();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Manga> mangaPage = mangaRepository.findRelatedByGenres(genreIds, uuid, pageable);
        List<MangaSummaryDTO> content = mangaPage.getContent().stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
        return PagedResponseDTO.from(mangaPage, content);
    }
}
