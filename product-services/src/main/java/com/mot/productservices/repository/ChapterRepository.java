package com.mot.productservices.repository;

import com.mot.productservices.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Integer> {

    List<Chapter> findByMangaIdOrderByChapterNumberDesc(UUID mangaId);

    @Query("SELECT c FROM Chapter c LEFT JOIN FETCH c.images WHERE c.id = :id AND c.manga.id = :mangaId")
    Optional<Chapter> findByIdAndMangaIdWithImages(@Param("id") Integer id, @Param("mangaId") UUID mangaId);

    Optional<Chapter> findByUrl(String url);

    @Query("SELECT c FROM Chapter c WHERE c.manga.id = :mangaId ORDER BY c.chapterNumber DESC")
    List<Chapter> findLatestChapters(@Param("mangaId") UUID mangaId);

    @Query("SELECT MAX(c.chapterNumber) FROM Chapter c WHERE c.manga.id = :mangaId")
    Double findMaxChapterNumber(@Param("mangaId") UUID mangaId);

    @Query("SELECT c FROM Chapter c WHERE c.manga.id = :mangaId AND c.chapterNumber < :chapterNumber ORDER BY c.chapterNumber DESC")
    List<Chapter> findPrevChapter(@Param("mangaId") UUID mangaId, @Param("chapterNumber") Double chapterNumber);

    @Query("SELECT c FROM Chapter c WHERE c.manga.id = :mangaId AND c.chapterNumber > :chapterNumber ORDER BY c.chapterNumber ASC")
    List<Chapter> findNextChapter(@Param("mangaId") UUID mangaId, @Param("chapterNumber") Double chapterNumber);
}
