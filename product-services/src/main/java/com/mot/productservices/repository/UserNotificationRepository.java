package com.mot.productservices.repository;

import com.mot.productservices.entity.UserNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    Page<UserNotification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    long countByUserIdAndReadAtIsNull(String userId);

    @Modifying
    @Query("UPDATE UserNotification n SET n.readAt = :now WHERE n.userId = :userId AND n.readAt IS NULL")
    int markAllRead(@Param("userId") String userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query(value = """
        INSERT INTO user_notification (user_id, manga_id, chapter_id, type, title, created_at)
        SELECT uf.user_id, c.manga_id, c.id, 'NEW_CHAPTER',
               m.title || ' có Chương ' || REGEXP_REPLACE(c.chapter_number::text, '\\.0$', ''), NOW()
        FROM chapter c
        JOIN manga m ON m.id = c.manga_id
        JOIN user_favorite uf ON uf.manga_id = c.manga_id
        WHERE c.id = :chapterId
        ON CONFLICT (user_id, chapter_id, type) DO NOTHING
        """, nativeQuery = true)
    int createForNewChapter(@Param("chapterId") Integer chapterId);
}
