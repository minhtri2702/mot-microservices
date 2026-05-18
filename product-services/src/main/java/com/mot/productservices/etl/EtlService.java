package com.mot.productservices.etl;

import com.mot.productservices.crawler.entity.*;
import com.mot.productservices.crawler.repository.*;
import com.mot.productservices.entity.*;
import com.mot.productservices.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ETL Service: Migrates data from crawler_db (read-only) to mot_db (product database).
 * Runs on startup to sync all data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EtlService {

    private final CrawlerMangaRepository crawlerMangaRepository;
    private final CrawlerChapterRepository crawlerChapterRepository;
    private final CrawlerChapterImageRepository crawlerChapterImageRepository;
    private final CrawlerGenreRepository crawlerGenreRepository;
    private final CrawlerMangaGenreRepository crawlerMangaGenreRepository;

    private final MangaRepository mangaRepository;
    private final ChapterRepository chapterRepository;
    private final ChapterImageRepository chapterImageRepository;
    private final GenreRepository genreRepository;

    @PostConstruct
    public void init() {
        log.info("=== Starting ETL: Crawler DB -> Product DB ===");
        try {
            runEtl();
            log.info("=== ETL completed successfully ===");
        } catch (Exception e) {
            log.error("ETL failed: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void runEtl() {
        // 1. Sync genres
        syncGenres();

        // 2. Sync manga (with genres)
        syncManga();

        // 3. Sync chapters and images
        syncChaptersAndImages();

        log.info("ETL sync completed.");
    }

    private void syncGenres() {
        List<CrawlerGenre> crawlerGenres = crawlerGenreRepository.findAll();
        log.info("Syncing {} genres...", crawlerGenres.size());

        for (CrawlerGenre cg : crawlerGenres) {
            if (genreRepository.findById(cg.getId()).isEmpty()) {
                Genre genre = Genre.builder()
                        .id(cg.getId())
                        .name(cg.getName())
                        .slug(cg.getSlug())
                        .build();
                genreRepository.save(genre);
            }
        }
    }

    private void syncManga() {
        List<CrawlerManga> crawlerMangas = crawlerMangaRepository.findAll();
        log.info("Syncing {} manga...", crawlerMangas.size());

        // Pre-load all manga_genre relationships
        List<CrawlerMangaGenre> allMangaGenres = crawlerMangaGenreRepository.findAll();
        Map<String, List<Integer>> mangaGenreMap = allMangaGenres.stream()
                .collect(Collectors.groupingBy(
                        CrawlerMangaGenre::getMangaId,
                        Collectors.mapping(CrawlerMangaGenre::getGenreId, Collectors.toList())
                ));

        for (CrawlerManga cm : crawlerMangas) {
            if (mangaRepository.findById(cm.getId()).isPresent()) {
                continue; // Skip if already synced
            }

            List<Genre> genres = mangaGenreMap
                    .getOrDefault(cm.getId().toString(), List.of())
                    .stream()
                    .map(genreId -> genreRepository.findById(genreId).orElse(null))
                    .filter(g -> g != null)
                    .collect(Collectors.toList());

            Manga manga = Manga.builder()
                    .id(cm.getId())
                    .stt(cm.getStt())
                    .title(cm.getTitle())
                    .url(cm.getUrl())
                    .coverImagePath(cm.getCoverImagePath())
                    .status(cm.getStatus())
                    .description(cm.getDescription())
                    .author(cm.getAuthor())
                    .alternativeTitles(cm.getAlternativeTitles())
                    .createdDate(cm.getCreatedDate())
                    .translationTeam(cm.getTranslationTeam())
                    .ageRating(cm.getAgeRating())
                    .likes(cm.getLikes())
                    .followers(cm.getFollowers())
                    .views(cm.getViews())
                    .maxChapterCrawled(cm.getMaxChapterCrawled())
                    .genres(genres)
                    .build();

            mangaRepository.save(manga);
        }
    }

    private void syncChaptersAndImages() {
        List<CrawlerManga> crawlerMangas = crawlerMangaRepository.findAll();
        log.info("Syncing chapters and images for {} manga...", crawlerMangas.size());

        for (CrawlerManga cm : crawlerMangas) {
            UUID mangaId = cm.getId();

            // Check if manga exists in product DB
            if (mangaRepository.findById(mangaId).isEmpty()) {
                log.warn("Manga {} not found in product DB, skipping chapters", mangaId);
                continue;
            }

            Manga manga = mangaRepository.findById(mangaId).get();
            List<CrawlerChapter> crawlerChapters = crawlerChapterRepository.findByMangaId(mangaId.toString());

            for (CrawlerChapter cc : crawlerChapters) {
                // Skip if chapter already exists
                if (chapterRepository.findById(cc.getId()).isPresent()) {
                    continue;
                }

                Chapter chapter = Chapter.builder()
                        .id(cc.getId())
                        .manga(manga)
                        .chapterNumber(cc.getChapterNumber())
                        .chapterName(cc.getChapterName())
                        .url(cc.getUrl())
                        .build();

                // Sync images for this chapter
                List<CrawlerChapterImage> crawlerImages = crawlerChapterImageRepository.findByChapterId(cc.getId());
                List<ChapterImage> images = crawlerImages.stream()
                        .map(ci -> ChapterImage.builder()
                                .imageUrl(ci.getImageUrl())
                                .imagePath(ci.getImagePath())
                                .pageOrder(ci.getPageOrder())
                                .build())
                        .collect(Collectors.toList());

                chapter.setImages(images);
                chapterRepository.save(chapter);
            }
        }
    }
}
