package com.mot.productservices.repository;

import com.mot.productservices.entity.Manga;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public interface MangaRepository extends JpaRepository<Manga, UUID> {

    @EntityGraph(attributePaths = {"genres"})
    @Query("SELECT m FROM Manga m WHERE m.id = :id")
    Optional<Manga> findByIdWithGenres(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Manga m SET m.views = m.views + 1 WHERE m.id = :id")
    void incrementViewCount(@Param("id") UUID id);

    Optional<Manga> findByUrl(String url);

    Optional<Manga> findByStt(Integer stt);

    // Search by title (case-insensitive) - fallback
    @Query("SELECT m FROM Manga m WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Manga> searchByTitle(@Param("keyword") String keyword, Pageable pageable);

    // Full-Text Search using PostgreSQL tsvector
    // Search across title (weight A), alternative_titles (weight B), author (weight C)
    // Sorted by relevance (ts_rank) then views
    @Query(value = "SELECT m.* FROM manga m " +
           "WHERE m.search_vector @@ plainto_tsquery('simple', :keyword) " +
           "ORDER BY ts_rank(m.search_vector, plainto_tsquery('simple', :keyword)) DESC, m.views DESC",
           countQuery = "SELECT COUNT(*) FROM manga m WHERE m.search_vector @@ plainto_tsquery('simple', :keyword)",
           nativeQuery = true)
    Page<Manga> searchByFullText(@Param("keyword") String keyword, Pageable pageable);

    // Fuzzy search fallback using trigram similarity (cho lỗi chính tả)
    @Query(value = "SELECT m.*, similarity(m.title, :keyword) AS sim " +
           "FROM manga m " +
           "WHERE m.title % :keyword OR m.author % :keyword " +
           "ORDER BY sim DESC, m.views DESC",
           countQuery = "SELECT COUNT(*) FROM manga m WHERE m.title % :keyword OR m.author % :keyword",
           nativeQuery = true)
    Page<Manga> searchByFuzzy(@Param("keyword") String keyword, Pageable pageable);

    // Latest updated manga
    Page<Manga> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    // Most viewed manga
    Page<Manga> findAllByOrderByViewsDesc(Pageable pageable);

    // Newest manga
    Page<Manga> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Completed manga
    Page<Manga> findByStatus(String status, Pageable pageable);

    // Featured manga (top views) - first get IDs with limit, then fetch with genres
    @Query("SELECT m.id FROM Manga m ORDER BY m.views DESC")
    List<UUID> findTopMangaIds(Pageable pageable);
    
    @EntityGraph(attributePaths = {"genres"})
    @Query("SELECT m FROM Manga m WHERE m.id IN :ids ORDER BY m.views DESC")
    List<Manga> findMangaByIdsWithGenres(@Param("ids") List<UUID> ids);
    
    // Batch query: get latest chapter number + updated_at for multiple manga IDs
    // Uses a single efficient query with DISTINCT ON
    @Query(value = "SELECT c.manga_id, c.chapter_number, CAST(c.updated_at AS VARCHAR) FROM chapter c " +
           "WHERE c.manga_id IN :ids " +
           "AND (c.manga_id, c.chapter_number) IN " +
           "(SELECT c2.manga_id, MAX(c2.chapter_number) FROM chapter c2 WHERE c2.manga_id IN :ids GROUP BY c2.manga_id)", nativeQuery = true)
    List<Object[]> findLatestChaptersBatch(@Param("ids") List<UUID> ids);

    // Manga by genre
    @Query("SELECT m FROM Manga m JOIN m.genres g WHERE g.id = :genreId ORDER BY m.updatedAt DESC")
    Page<Manga> findByGenreId(@Param("genreId") Integer genreId, Pageable pageable);

    // Related manga (same genres, excluding current)
    @Query(value = "SELECT m.* FROM manga m " +
            "JOIN manga_genre mg ON m.id = mg.manga_id " +
            "WHERE mg.genre_id IN (SELECT mg2.genre_id FROM manga_genre mg2 WHERE mg2.manga_id = :mangaId) " +
            "AND m.id != :mangaId " +
            "GROUP BY m.id " +
            "ORDER BY COUNT(mg.genre_id) DESC, m.updated_at DESC",
            nativeQuery = true)
    Page<Manga> findRelatedManga(@Param("mangaId") UUID mangaId, Pageable pageable);
}
