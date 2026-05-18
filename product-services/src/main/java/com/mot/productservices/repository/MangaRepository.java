 DApackage com.mot.productservices.repository;

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

    // Featured: top views
    List<Manga> findTop10ByOrderByViewsDesc();

    // Latest updated: order by updated_at desc
    Page<Manga> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    // Hot: order by views desc
    Page<Manga> findAllByOrderByViewsDesc(Pageable pageable);

    // New: order by created_at desc
    Page<Manga> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Completed
    Page<Manga> findByStatusContainingIgnoreCaseOrderByUpdatedAtDesc(String status, Pageable pageable);

    // Search by title
    @Query("SELECT m FROM Manga m WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY m.updatedAt DESC")
    Page<Manga> searchByTitle(@Param("keyword") String keyword, Pageable pageable);

    // Find by genre
    @Query("SELECT m FROM Manga m JOIN m.genres g WHERE g.id = :genreId ORDER BY m.updatedAt DESC")
    Page<Manga> findByGenreId(@Param("genreId") Integer genreId, Pageable pageable);

    // Find related manga by genre (exclude current manga)
    @Query("SELECT m FROM Manga m JOIN m.genres g WHERE g.id IN :genreIds AND m.id <> :mangaId ORDER BY m.views DESC")
    Page<Manga> findRelatedByGenres(@Param("genreIds") List<Integer> genreIds, @Param("mangaId") UUID mangaId, Pageable pageable);
}
