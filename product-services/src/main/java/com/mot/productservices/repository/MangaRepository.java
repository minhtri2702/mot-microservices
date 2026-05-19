package com.mot.productservices.repository;

import com.mot.productservices.entity.Manga;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MangaRepository extends JpaRepository<Manga, UUID> {

    Optional<Manga> findByUrl(String url);

    Optional<Manga> findByStt(Integer stt);

    // Search by title (case-insensitive)
    @Query("SELECT m FROM Manga m WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Manga> searchByTitle(@Param("keyword") String keyword, Pageable pageable);

    // Latest updated manga
    Page<Manga> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    // Most viewed manga
    Page<Manga> findAllByOrderByViewsDesc(Pageable pageable);

    // Newest manga
    Page<Manga> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Completed manga
    Page<Manga> findByStatus(String status, Pageable pageable);

    // Featured manga (top views)
    List<Manga> findTop6ByOrderByViewsDesc();

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
