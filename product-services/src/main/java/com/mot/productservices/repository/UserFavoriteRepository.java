package com.mot.productservices.repository;

import com.mot.productservices.entity.UserFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {

    Optional<UserFavorite> findByUserIdAndMangaId(String userId, UUID mangaId);

    boolean existsByUserIdAndMangaId(String userId, UUID mangaId);

    @Modifying
    @Query("DELETE FROM UserFavorite uf WHERE uf.userId = :userId AND uf.mangaId = :mangaId")
    void deleteByUserIdAndMangaId(@Param("userId") String userId, @Param("mangaId") UUID mangaId);

    @Query(value = """
        SELECT uf.manga_id, m.stt, m.title, m.cover_image_path, m.author, m.status,
               m.views, m.likes, m.followers, m.max_chapter_crawled, m.updated_at
        FROM user_favorite uf
        JOIN manga m ON m.id = uf.manga_id
        WHERE uf.user_id = :userId
        ORDER BY uf.created_at DESC
    """, countQuery = """
        SELECT COUNT(*) FROM user_favorite uf WHERE uf.user_id = :userId
    """, nativeQuery = true)
    Page<Object[]> findFavoritesByUserId(@Param("userId") String userId, Pageable pageable);

    @Modifying
    @Query("UPDATE Manga m SET m.followers = m.followers + 1 WHERE m.id = :mangaId")
    void incrementFollowerCount(@Param("mangaId") UUID mangaId);

    @Modifying
    @Query("UPDATE Manga m SET m.followers = m.followers - 1 WHERE m.id = :mangaId")
    void decrementFollowerCount(@Param("mangaId") UUID mangaId);
}
