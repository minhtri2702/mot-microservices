package com.mot.productservices.repository;

import com.mot.productservices.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Integer> {

    interface DataHealthIssueProjection {
        Integer getChapterId();
        String getMangaId();
        String getMangaTitle();
        Double getChapterNumber();
        String getChapterName();
        Integer getImageCount();
    }

    List<Chapter> findByMangaIdOrderByChapterNumberDesc(UUID mangaId);

    @Query("SELECT c FROM Chapter c LEFT JOIN FETCH c.images WHERE c.id = :id AND c.manga.id = :mangaId")
    Optional<Chapter> findByIdAndMangaIdWithImages(@Param("id") Integer id, @Param("mangaId") UUID mangaId);

    Optional<Chapter> findByUrl(String url);

    @Query("SELECT c FROM Chapter c WHERE c.manga.id = :mangaId ORDER BY c.chapterNumber DESC")
    List<Chapter> findLatestChapters(@Param("mangaId") UUID mangaId);

    @Query("SELECT MAX(c.chapterNumber) FROM Chapter c WHERE c.manga.id = :mangaId")
    Double findMaxChapterNumber(@Param("mangaId") UUID mangaId);

    @Query(value = """
            SELECT c.id AS chapterId,
                   CAST(m.id AS varchar) AS mangaId,
                   m.title AS mangaTitle,
                   c.chapter_number AS chapterNumber,
                   c.chapter_name AS chapterName,
                   0 AS imageCount
            FROM chapter c
            JOIN manga m ON m.id = c.manga_id
            WHERE NOT EXISTS (SELECT 1 FROM chapter_image ci WHERE ci.chapter_id = c.id)
            ORDER BY c.updated_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM chapter c
            WHERE NOT EXISTS (SELECT 1 FROM chapter_image ci WHERE ci.chapter_id = c.id)
            """,
            nativeQuery = true)
    Page<DataHealthIssueProjection> findChaptersWithoutImages(Pageable pageable);

    @Query(value = """
            SELECT COUNT(*) FROM chapter c
            WHERE NOT EXISTS (SELECT 1 FROM chapter_image ci WHERE ci.chapter_id = c.id)
            """, nativeQuery = true)
    long countChaptersWithoutImages();

    @Query(value = """
            SELECT COUNT(*) FROM chapter_image ci
            WHERE (ci.image_url IS NULL OR BTRIM(ci.image_url) = '')
              AND (ci.image_path IS NULL OR BTRIM(ci.image_path) = '')
            """, nativeQuery = true)
    long countImagesWithoutPath();

    @Query(value = """
            SELECT COUNT(*) FROM (
                SELECT ci.chapter_id, ci.page_order
                FROM chapter_image ci
                GROUP BY ci.chapter_id, ci.page_order
                HAVING COUNT(*) > 1
            ) duplicate_pages
            """, nativeQuery = true)
    long countDuplicatePageOrders();

    @Query("SELECT c FROM Chapter c WHERE c.manga.id = :mangaId AND c.chapterNumber < :chapterNumber ORDER BY c.chapterNumber DESC")
    List<Chapter> findPrevChapter(@Param("mangaId") UUID mangaId, @Param("chapterNumber") Double chapterNumber);

    @Query("SELECT c FROM Chapter c WHERE c.manga.id = :mangaId AND c.chapterNumber > :chapterNumber ORDER BY c.chapterNumber ASC")
    List<Chapter> findNextChapter(@Param("mangaId") UUID mangaId, @Param("chapterNumber") Double chapterNumber);
}
