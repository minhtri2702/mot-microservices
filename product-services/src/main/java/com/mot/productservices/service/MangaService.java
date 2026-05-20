package com.mot.productservices.service;

import com.mot.productservices.dto.*;
import com.mot.productservices.entity.Chapter;
import com.mot.productservices.entity.ChapterImage;
import com.mot.productservices.entity.Genre;
import com.mot.productservices.entity.Manga;
import com.mot.productservices.repository.ChapterRepository;
import com.mot.productservices.repository.GenreRepository;
import com.mot.productservices.repository.MangaRepository;
import com.mot.productservices.repository.UserChapterStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MangaService {

    private final MangaRepository mangaRepository;
    private final ChapterRepository chapterRepository;
    private final GenreRepository genreRepository;
    private final MinioService minioService;
    private final UserChapterStatusRepository userChapterStatusRepository;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // ==================== Manga Listing ====================

    @Transactional(readOnly = true)
    public List<MangaSummaryDTO> getFeaturedManga() {
        // Step 1: Get only IDs with LIMIT (efficient single-column query)
        List<UUID> topIds = mangaRepository.findTopMangaIds(PageRequest.of(0, 6));
        if (topIds.isEmpty()) return Collections.emptyList();
        
        // Step 2: Fetch full entities with genres for those IDs only
        List<Manga> mangas = mangaRepository.findMangaByIdsWithGenres(topIds);
        return toSummaryDTOs(mangas);
    }

    @Transactional(readOnly = true)
    public PagedResponseDTO<MangaSummaryDTO> getLatestUpdated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Manga> mangaPage = mangaRepository.findAllByOrderByUpdatedAtDesc(pageable);
        return toPagedResponse(mangaPage);
    }

    @Transactional(readOnly = true)
    public PagedResponseDTO<MangaSummaryDTO> getHotManga(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Manga> mangaPage = mangaRepository.findAllByOrderByViewsDesc(pageable);
        return toPagedResponse(mangaPage);
    }

    @Transactional(readOnly = true)
    public PagedResponseDTO<MangaSummaryDTO> getNewManga(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Manga> mangaPage = mangaRepository.findAllByOrderByCreatedAtDesc(pageable);
        return toPagedResponse(mangaPage);
    }

    @Transactional(readOnly = true)
    public PagedResponseDTO<MangaSummaryDTO> getCompletedManga(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Manga> mangaPage = mangaRepository.findByStatus("Hoàn thành", pageable);
        return toPagedResponse(mangaPage);
    }

    // ==================== Manga Detail ====================

    @Transactional
    public MangaDetailDTO getMangaDetail(UUID id) {
        // Fetch manga with genres in 1 query
        Manga manga = mangaRepository.findByIdWithGenres(id)
                .orElseThrow(() -> new NoSuchElementException("Manga not found with id: " + id));

        // Fetch chapters in a separate query (avoids Cartesian product with genres)
        List<Chapter> chapters = chapterRepository.findByMangaIdOrderByChapterNumberDesc(id);

        // Increment view count with a single UPDATE query (no need to load + save entity)
        mangaRepository.incrementViewCount(id);

        return toDetailDTO(manga, chapters);
    }

    // ==================== Chapter ====================

    @Transactional
    public ChapterDetailDTO getChapterDetail(UUID mangaId, Integer chapterId) {
        Chapter chapter = chapterRepository.findByIdAndMangaIdWithImages(chapterId, mangaId)
                .orElseThrow(() -> new NoSuchElementException("Chapter not found"));

        // Increment view count
        chapter.setViewCount(chapter.getViewCount() == null ? 1L : chapter.getViewCount() + 1L);
        chapterRepository.save(chapter);

        // Get navigation
        ChapterNavigationDTO navigation = getChapterNavigation(mangaId, chapter.getChapterNumber());

        // Get image URLs - convert to presigned URLs for direct MinIO access
        List<String> imageUrls = chapter.getImages().stream()
                .sorted(Comparator.comparing(ChapterImage::getPageOrder))
                .map(img -> {
                    String path = img.getImagePath() != null ? img.getImagePath() : img.getImageUrl();
                    return path != null ? minioService.getPresignedUrl(path) : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return ChapterDetailDTO.builder()
                .id(chapter.getId())
                .chapterNumber(chapter.getChapterNumber())
                .chapterName(chapter.getChapterName())
                .viewCount(chapter.getViewCount())
                .createdAt(chapter.getCreatedAt() != null ? chapter.getCreatedAt().format(DTF) : null)
                .imageUrls(imageUrls)
                .navigation(navigation)
                .build();
    }

    private ChapterNavigationDTO getChapterNavigation(UUID mangaId, Double currentChapterNumber) {
        List<Chapter> prevChapters = chapterRepository.findPrevChapter(mangaId, currentChapterNumber);
        List<Chapter> nextChapters = chapterRepository.findNextChapter(mangaId, currentChapterNumber);

        Chapter prev = prevChapters.isEmpty() ? null : prevChapters.get(0);
        Chapter next = nextChapters.isEmpty() ? null : nextChapters.get(0);

        return ChapterNavigationDTO.builder()
                .prevChapterId(prev != null ? prev.getId() : null)
                .prevChapterNumber(prev != null ? prev.getChapterNumber() : null)
                .nextChapterId(next != null ? next.getId() : null)
                .nextChapterNumber(next != null ? next.getChapterNumber() : null)
                .build();
    }

    // ==================== Search ====================

    public PagedResponseDTO<MangaSummaryDTO> searchManga(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Manga> mangaPage = mangaRepository.searchByTitle(keyword, pageable);
        return toPagedResponse(mangaPage);
    }

    // ==================== Genre ====================

    public List<GenreDTO> getAllGenres() {
        List<Genre> genres = genreRepository.findAll();
        return genres.stream()
                .map(g -> GenreDTO.builder()
                        .id(g.getId())
                        .name(g.getName())
                        .slug(g.getSlug())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PagedResponseDTO<MangaSummaryDTO> getMangaByGenre(Integer genreId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Manga> mangaPage = mangaRepository.findByGenreId(genreId, pageable);
        return toPagedResponse(mangaPage);
    }

    // ==================== Related ====================

    @Transactional(readOnly = true)
    public PagedResponseDTO<MangaSummaryDTO> getRelatedManga(UUID mangaId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Manga> mangaPage = mangaRepository.findRelatedManga(mangaId, pageable);
        return toPagedResponse(mangaPage);
    }

    // ==================== Reading History ====================

    @Transactional(readOnly = true)
    public List<ReadingHistoryDTO> getReadingHistory(UUID userId, int limit) {
        List<Object[]> rows = userChapterStatusRepository.findReadingHistory(userId, limit);
        if (rows.isEmpty()) return Collections.emptyList();

        return rows.stream().map(row -> {
            Integer chapterId = ((Number) row[0]).intValue();
            Object lastReadObj = row[1];
            UUID mangaId = (UUID) row[2];
            Double chapterNumber = ((Number) row[3]).doubleValue();
            String chapterName = (String) row[4];
            String mangaTitle = (String) row[5];
            String coverPath = (String) row[6];
            Integer stt = row[7] != null ? ((Number) row[7]).intValue() : null;

            // Handle both java.sql.Timestamp and java.time.Instant
            java.time.LocalDateTime lastReadLdt;
            if (lastReadObj instanceof java.sql.Timestamp) {
                lastReadLdt = ((java.sql.Timestamp) lastReadObj).toLocalDateTime();
            } else if (lastReadObj instanceof java.time.Instant) {
                lastReadLdt = ((java.time.Instant) lastReadObj).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            } else if (lastReadObj instanceof java.time.LocalDateTime) {
                lastReadLdt = (java.time.LocalDateTime) lastReadObj;
            } else {
                lastReadLdt = java.time.LocalDateTime.now();
            }

            String coverUrl = coverPath != null ? minioService.getPresignedUrl(coverPath) : null;

            return ReadingHistoryDTO.builder()
                    .mangaId(mangaId.toString())
                    .mangaTitle(mangaTitle)
                    .coverImagePath(coverUrl)
                    .stt(stt)
                    .chapterId(chapterId)
                    .chapterNumber(chapterNumber)
                    .chapterName(chapterName)
                    .lastReadDate(lastReadLdt.format(DTF))
                    .build();
        }).collect(Collectors.toList());
    }

    // ==================== Batch Mappers (N+1 fix) ====================

    /**
     * Batch convert list of Manga to SummaryDTOs using data from manga table directly
     * (max_chapter_crawled + updated_at) - no need to query chapter table
     */
    private List<MangaSummaryDTO> toSummaryDTOs(List<Manga> mangas) {
        if (mangas.isEmpty()) return Collections.emptyList();

        return mangas.stream().map(manga -> {
            String coverUrl = manga.getCoverImagePath() != null
                    ? minioService.getPresignedUrl(manga.getCoverImagePath())
                    : null;

            Double latestChapter = manga.getMaxChapterCrawled() != null
                    ? manga.getMaxChapterCrawled().doubleValue()
                    : null;

            String latestChapterUpdatedAt = manga.getUpdatedAt() != null
                    ? manga.getUpdatedAt().format(DTF)
                    : null;

            return MangaSummaryDTO.builder()
                    .id(manga.getId().toString())
                    .stt(manga.getStt())
                    .title(manga.getTitle())
                    .coverImagePath(coverUrl)
                    .status(manga.getStatus())
                    .author(manga.getAuthor())
                    .views(manga.getViews())
                    .likes(manga.getLikes())
                    .followers(manga.getFollowers())
                    .latestChapter(latestChapter)
                    .latestChapterUpdatedAt(latestChapterUpdatedAt)
                    .genres(manga.getGenres() != null
                            ? manga.getGenres().stream().map(Genre::getName).collect(Collectors.toList())
                            : Collections.emptyList())
                    .build();
        }).collect(Collectors.toList());
    }

    private MangaSummaryDTO toSummaryDTO(Manga manga) {
        return toSummaryDTOs(Collections.singletonList(manga)).get(0);
    }

    private MangaDetailDTO toDetailDTO(Manga manga, List<Chapter> chapters) {
        Double latestChapter = null;
        String latestChapterUpdatedAt = null;
        if (chapters != null && !chapters.isEmpty()) {
            Chapter max = chapters.stream()
                    .max(Comparator.comparing(Chapter::getChapterNumber))
                    .orElse(null);
            if (max != null) {
                latestChapter = max.getChapterNumber();
                latestChapterUpdatedAt = max.getUpdatedAt() != null ? max.getUpdatedAt().format(DTF) : null;
            }
        }

        List<ChapterSummaryDTO> chapterDTOs = chapters != null ? chapters.stream()
                .sorted(Comparator.comparing(Chapter::getChapterNumber).reversed())
                .map(ch -> ChapterSummaryDTO.builder()
                        .id(ch.getId())
                        .chapterNumber(ch.getChapterNumber())
                        .chapterName(ch.getChapterName())
                        .viewCount(ch.getViewCount())
                        .createdAt(ch.getCreatedAt() != null ? ch.getCreatedAt().format(DTF) : null)
                        .build())
                .collect(Collectors.toList()) : Collections.emptyList();

        String coverUrl = manga.getCoverImagePath() != null
                ? minioService.getPresignedUrl(manga.getCoverImagePath())
                : null;

        return MangaDetailDTO.builder()
                .id(manga.getId().toString())
                .stt(manga.getStt())
                .title(manga.getTitle())
                .coverImagePath(coverUrl)
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
                .genres(manga.getGenres().stream().map(Genre::getName).collect(Collectors.toList()))
                .chapters(chapterDTOs)
                .build();
    }

    private PagedResponseDTO<MangaSummaryDTO> toPagedResponse(Page<Manga> page) {
        List<MangaSummaryDTO> content = toSummaryDTOs(page.getContent());

        return PagedResponseDTO.<MangaSummaryDTO>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
