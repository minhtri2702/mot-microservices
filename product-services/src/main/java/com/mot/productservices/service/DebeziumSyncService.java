package com.mot.productservices.service;

import com.mot.productservices.dto.DebeziumEvent;
import com.mot.productservices.entity.Chapter;
import com.mot.productservices.entity.ChapterImage;
import com.mot.productservices.entity.Manga;
import com.mot.productservices.repository.ChapterRepository;
import com.mot.productservices.repository.ChapterImageRepository;
import com.mot.productservices.repository.MangaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service xử lý event từ Debezium.
 * 
 * Khi crawl ghi dữ liệu vào crawler_db, Debezium phát hiện thay đổi
 * và push event vào Kafka. Service này sẽ:
 * 1. Nhận event
 * 2. Đồng bộ dữ liệu vào DB của product-services (mot_db)
 * 3. Xoá cache tương ứng
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DebeziumSyncService {

    private final MangaRepository mangaRepository;
    private final ChapterRepository chapterRepository;
    private final ChapterImageRepository chapterImageRepository;
    private final CacheManager cacheManager;

    /**
     * Xử lý event từ bảng manga.
     * Đồng bộ dữ liệu từ crawler_db sang mot_db.
     */
    @Transactional
    public void processMangaEvent(DebeziumEvent event) {
        String operation = event.getOperation();
        String mangaIdStr = event.getMangaId();

        // Xử lý DELETE trước, vì DELETE không có 'after' data
        if ("DELETE".equals(operation)) {
            if (mangaIdStr != null) {
                UUID mangaId = UUID.fromString(mangaIdStr);
                log.info("Processing DELETE event for manga: {}", mangaId);
                handleMangaDelete(mangaId);
            } else {
                log.warn("Cannot process DELETE event: no manga ID available in 'before' data");
            }
            return;
        }

        // CREATE/UPDATE: cần có 'after' data
        DebeziumEvent.After after = event.getPayload().getAfter();
        if (after == null) {
            log.warn("Received manga event with null 'after' data, operation: {}", operation);
            return;
        }

        UUID mangaId = UUID.fromString(after.getId());

        switch (operation) {
            case "CREATE" -> handleMangaCreate(after, mangaId);
            case "UPDATE" -> handleMangaUpdate(after, mangaId);
            default -> log.warn("Unknown operation: {}", operation);
        }
    }

    private void handleMangaCreate(DebeziumEvent.After after, UUID mangaId) {
        // Kiểm tra xem manga đã tồn tại trong mot_db chưa
        if (mangaRepository.existsById(mangaId)) {
            log.info("Manga {} already exists in mot_db, skipping create", mangaId);
            return;
        }

        // Tạo mới manga trong mot_db
        Manga manga = Manga.builder()
                .id(mangaId)
                .stt(after.getStt())
                .title(after.getTitle())
                .url(after.getUrl())
                .coverImagePath(after.getCoverImagePath())
                .status(after.getStatus())
                .description(after.getDescription())
                .author(after.getAuthor())
                .alternativeTitles(after.getAlternativeTitles())
                .createdDate(after.getCreatedDate())
                .translationTeam(after.getTranslationTeam())
                .ageRating(after.getAgeRating())
                .likes(after.getLikes() != null ? after.getLikes() : 0L)
                .followers(after.getFollowers() != null ? after.getFollowers() : 0L)
                .views(after.getViews() != null ? after.getViews() : 0L)
                .maxChapterCrawled(after.getMaxChapterCrawled())
                .build();

        mangaRepository.save(manga);
        log.info("Synced new manga to mot_db: {} (ID: {})", after.getTitle(), mangaId);

        // Xoá cache danh sách (vì có manga mới)
        evictCache("mangaListings");
    }

    private void handleMangaUpdate(DebeziumEvent.After after, UUID mangaId) {
        Optional<Manga> existing = mangaRepository.findById(mangaId);
        if (existing.isEmpty()) {
            log.warn("Manga {} not found in mot_db for update, creating instead", mangaId);
            handleMangaCreate(after, mangaId);
            return;
        }

        Manga manga = existing.get();
        manga.setStt(after.getStt());
        manga.setTitle(after.getTitle());
        manga.setUrl(after.getUrl());
        manga.setCoverImagePath(after.getCoverImagePath());
        manga.setStatus(after.getStatus());
        manga.setDescription(after.getDescription());
        manga.setAuthor(after.getAuthor());
        manga.setAlternativeTitles(after.getAlternativeTitles());
        manga.setCreatedDate(after.getCreatedDate());
        manga.setTranslationTeam(after.getTranslationTeam());
        manga.setAgeRating(after.getAgeRating());
        manga.setLikes(after.getLikes() != null ? after.getLikes() : 0L);
        manga.setFollowers(after.getFollowers() != null ? after.getFollowers() : 0L);
        manga.setViews(after.getViews() != null ? after.getViews() : 0L);
        manga.setMaxChapterCrawled(after.getMaxChapterCrawled());

        mangaRepository.save(manga);
        log.info("Updated manga in mot_db: {} (ID: {})", after.getTitle(), mangaId);

        // Xoá cache detail + danh sách
        evictCacheEntry("mangaDetail", mangaId.toString());
        evictCache("mangaListings");
    }

    private void handleMangaDelete(UUID mangaId) {
        if (mangaRepository.existsById(mangaId)) {
            mangaRepository.deleteById(mangaId);
            log.info("Deleted manga from mot_db: {}", mangaId);
        }

        // Xoá cache
        evictCacheEntry("mangaDetail", mangaId.toString());
        evictCache("mangaListings");
    }

    /**
     * Xử lý event từ bảng chapter.
     * Đồng bộ chapter từ crawler_db sang mot_db.
     */
    @Transactional
    public void processChapterEvent(DebeziumEvent event) {
        String operation = event.getOperation();
        String mangaIdStr = event.getMangaIdFromChapter();

        // DELETE: xoá chapter khỏi mot_db
        if ("DELETE".equals(operation)) {
            log.info("Processing DELETE event for chapter (mangaId={})", mangaIdStr);
            // Chapter dùng id tự tăng (Integer), không có UUID -> cần xoá cache
            if (mangaIdStr != null) {
                evictCacheEntry("mangaDetail", mangaIdStr);
                evictCache("mangaListings");
            }
            return;
        }

        // CREATE/UPDATE: cần có 'after' data
        DebeziumEvent.After after = event.getPayload().getAfter();
        if (after == null || mangaIdStr == null) {
            log.warn("Received chapter event with null 'after' or mangaId, operation: {}", operation);
            return;
        }

        UUID mangaId = UUID.fromString(mangaIdStr);

        // Kiểm tra manga đã tồn tại trong mot_db chưa
        if (!mangaRepository.existsById(mangaId)) {
            throw new IllegalStateException(
                    "Manga " + mangaId + " not available yet; retrying chapter event");
        }

        switch (operation) {
            case "CREATE" -> handleChapterCreate(after, mangaId);
            case "UPDATE" -> handleChapterUpdate(after, mangaId);
            default -> log.warn("Unknown operation: {}", operation);
        }
    }

    private void handleChapterCreate(DebeziumEvent.After after, UUID mangaId) {
        // Kiểm tra chapter đã tồn tại qua url
        if (chapterRepository.findByUrl(after.getUrl()).isPresent()) {
            log.info("Chapter with url {} already exists, skipping", after.getUrl());
            return;
        }

        Manga manga = mangaRepository.getReferenceById(mangaId);
        Chapter chapter = Chapter.builder()
                .manga(manga)
                .chapterNumber(after.getChapterNumber())
                .chapterName(after.getChapterName())
                .url(after.getUrl())
                .viewCount(0L)
                .build();

        chapterRepository.save(chapter);
        log.info("Synced new chapter: {} (mangaId={})", after.getChapterName(), mangaId);

        // Xoá cache
        evictCacheEntry("mangaDetail", mangaId.toString());
        evictCache("mangaListings");
    }

    private void handleChapterUpdate(DebeziumEvent.After after, UUID mangaId) {
        Optional<Chapter> existing = chapterRepository.findByUrl(after.getUrl());
        if (existing.isEmpty()) {
            log.warn("Chapter with url {} not found, creating instead", after.getUrl());
            handleChapterCreate(after, mangaId);
            return;
        }

        Chapter chapter = existing.get();
        chapter.setChapterNumber(after.getChapterNumber());
        chapter.setChapterName(after.getChapterName());
        chapter.setUrl(after.getUrl());

        chapterRepository.save(chapter);
        log.info("Updated chapter: {} (mangaId={})", after.getChapterName(), mangaId);

        // Xoá cache
        evictCacheEntry("mangaDetail", mangaId.toString());
        evictCache("mangaListings");
    }

    /**
     * Xử lý event từ bảng chapter_image.
     * Chapter_image có composite key (chapter_id, page_order).
     * Khi chapter_image thay đổi, cần xoá cache chapter detail.
     */
    @Transactional
    public void processChapterImageEvent(DebeziumEvent event) {
        String operation = event.getOperation();
        DebeziumEvent.After after = event.getPayload().getAfter();
        DebeziumEvent.Before before = event.getPayload().getBefore();
        Integer chapterId = event.getChapterId();

        if (chapterId == null) {
            log.warn("Cannot process chapter_image event without chapter_id: op={}", operation);
            return;
        }

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new IllegalStateException(
                        "Chapter " + chapterId + " not available yet; retrying chapter_image event"));

        Integer pageOrder = after != null ? after.getPageOrder()
                : before != null ? before.getPageOrder() : null;
        if (pageOrder == null) {
            log.warn("Cannot process chapter_image event without page_order: chapterId={}", chapterId);
            return;
        }

        switch (operation) {
            case "CREATE", "UPDATE" -> {
                if (after == null || after.getImageUrl() == null) {
                    log.warn("Cannot upsert chapter_image with null after/image_url: chapterId={}, page={}",
                            chapterId, pageOrder);
                    return;
                }

                ChapterImage image = chapterImageRepository
                        .findByChapterIdAndPageOrder(chapterId, pageOrder)
                        .orElseGet(() -> ChapterImage.builder()
                                .chapter(chapter)
                                .pageOrder(pageOrder)
                                .build());
                image.setImageUrl(after.getImageUrl());
                image.setImagePath(after.getImagePath());
                chapterImageRepository.save(image);
                log.debug("Upserted chapter_image: chapterId={}, page={}", chapterId, pageOrder);
            }
            case "DELETE" -> {
                chapterImageRepository.deleteByChapterIdAndPageOrder(chapterId, pageOrder);
                log.debug("Deleted chapter_image: chapterId={}, page={}", chapterId, pageOrder);
            }
            default -> log.warn("Unknown chapter_image operation: {}", operation);
        }

        evictCacheEntry("mangaDetail", chapter.getManga().getId().toString());
    }

    /**
     * Xử lý event từ bảng manga_genre.
     * Khi genre mapping thay đổi, cần xoá cache danh sách.
     */
    @Transactional
    public void processMangaGenreEvent(DebeziumEvent event) {
        log.info("Manga genre changed, evicting listings cache");
        evictCache("mangaListings");
    }

    private void evictCache(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.debug("Evicted cache: {}", cacheName);
        }
    }

    private void evictCacheEntry(String cacheName, String key) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evictIfPresent(key);
            log.debug("Evicted cache entry: {}::{}", cacheName, key);
        }
    }
}
