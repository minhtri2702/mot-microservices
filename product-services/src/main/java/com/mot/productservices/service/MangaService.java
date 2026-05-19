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

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // ==================== Manga Listing ====================

    public List<MangaSummaryDTO> getFeaturedManga() {
        List<Manga> mangas = mangaRepository.findTop6ByOrderByViewsDesc();
        return mangas.stream().map(this::toSummaryDTO).collect(Collectors.toList());
    }

    public PagedResponseDTO<MangaSummaryDTO> getLatestUpdated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Manga> mangaPage = mangaRepository.findAllByOrderByUpdatedAtDesc(pageable);
        return toPagedResponse(mangaPage);
    }

    public PagedResponseDTO<MangaSummaryDTO> getHotManga(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Manga> mangaPage = mangaRepository.findAllByOrderByViewsDesc(pageable);
        return toPagedResponse(mangaPage);
    }

    public PagedResponseDTO<MangaSummaryDTO> getNewManga(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Manga> mangaPage = mangaRepository.findAllByOrderByCreatedAtDesc(pageable);
        return toPagedResponse(mangaPage);
    }

    public PagedResponseDTO<MangaSummaryDTO> getCompletedManga(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Manga> mangaPage = mangaRepository.findByStatus("Hoàn thành", pageable);
        return toPagedResponse(mangaPage);
    }

    // ==================== Manga Detail ====================

    public MangaDetailDTO getMangaDetail(UUID id) {
        Manga manga = mangaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Manga not found with id: " + id));

        // Increment view count
        manga.setViews(manga.getViews() + 1);
        mangaRepository.save(manga);

        return toDetailDTO(manga);
    }

    // ==================== Chapter ====================

    public ChapterDetailDTO getChapterDetail(UUID mangaId, Integer chapterId) {
        Chapter chapter = chapterRepository.findByIdAndMangaId(chapterId, mangaId)
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

    public PagedResponseDTO<MangaSummaryDTO> getMangaByGenre(Integer genreId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Manga> mangaPage = mangaRepository.findByGenreId(genreId, pageable);
        return toPagedResponse(mangaPage);
    }

    // ==================== Related ====================

    public PagedResponseDTO<MangaSummaryDTO> getRelatedManga(UUID mangaId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Manga> mangaPage = mangaRepository.findRelatedManga(mangaId, pageable);
        return toPagedResponse(mangaPage);
    }

    // ==================== Mappers ====================

    private MangaSummaryDTO toSummaryDTO(Manga manga) {
        Double latestChapter = chapterRepository.findMaxChapterNumber(manga.getId());

        // Get latest chapter's updated_at
        List<Chapter> latestChapters = chapterRepository.findLatestChapters(manga.getId());
        String latestChapterUpdatedAt = null;
        if (!latestChapters.isEmpty()) {
            Chapter ch = latestChapters.get(0);
            latestChapterUpdatedAt = ch.getUpdatedAt() != null ? ch.getUpdatedAt().format(DTF) : null;
        }

        String coverUrl = manga.getCoverImagePath() != null
                ? minioService.getPresignedUrl(manga.getCoverImagePath())
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
                .genres(manga.getGenres().stream().map(Genre::getName).collect(Collectors.toList()))
                .build();
    }

    private MangaDetailDTO toDetailDTO(Manga manga) {
        Double latestChapter = chapterRepository.findMaxChapterNumber(manga.getId());

        List<Chapter> latestChapters = chapterRepository.findLatestChapters(manga.getId());
        String latestChapterUpdatedAt = null;
        if (!latestChapters.isEmpty()) {
            Chapter ch = latestChapters.get(0);
            latestChapterUpdatedAt = ch.getUpdatedAt() != null ? ch.getUpdatedAt().format(DTF) : null;
        }

        List<ChapterSummaryDTO> chapterDTOs = manga.getChapters().stream()
                .sorted(Comparator.comparing(Chapter::getChapterNumber).reversed())
                .map(ch -> ChapterSummaryDTO.builder()
                        .id(ch.getId())
                        .chapterNumber(ch.getChapterNumber())
                        .chapterName(ch.getChapterName())
                        .viewCount(ch.getViewCount())
                        .createdAt(ch.getCreatedAt() != null ? ch.getCreatedAt().format(DTF) : null)
                        .build())
                .collect(Collectors.toList());

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
        List<MangaSummaryDTO> content = page.getContent().stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());

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
