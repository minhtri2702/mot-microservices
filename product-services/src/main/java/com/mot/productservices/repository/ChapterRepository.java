package com.mot.productservices.repository;

import com.mot.productservices.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Integer> {

    Optional<Chapter> findByIdAndMangaId(Integer id, UUID mangaId);

    @Query("SELECT c FROM Chapter c WHERE c.manga.id = :mangaId AND c.chapterNumber < :chapterNumber ORDER BY c.chapterNumber DESC LIMIT 1")
    Optional<Chapter> findPreviousChapter(@Param("mangaId") UUID mangaId, @Param("chapterNumber") Double chapterNumber);

    @Query("SELECT c FROM Chapter c WHERE c.manga.id = :mangaId AND c.chapterNumber > :chapterNumber ORDER BY c.chapterNumber ASC LIMIT 1")
    Optional<Chapter> findNextChapter(@Param("mangaId") UUID mangaId, @Param("chapterNumber") Double chapterNumber);
}
