package com.mot.productservices.repository;

import com.mot.productservices.entity.UserChapterStatus;
import com.mot.productservices.entity.UserChapterStatusId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserChapterStatusRepository extends JpaRepository<UserChapterStatus, UserChapterStatusId> {

    @Query(value = """
        SELECT ucs.chapter_id, ucs.last_read_date, c.manga_id, c.chapter_number, c.chapter_name,
               m.title, m.cover_image_path, m.stt
        FROM user_chapter_status ucs
        JOIN chapter c ON c.id = ucs.chapter_id
        JOIN manga m ON m.id = c.manga_id
        WHERE ucs.user_id = :userId
        AND ucs.last_read_date IS NOT NULL
        ORDER BY ucs.last_read_date DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<Object[]> findReadingHistory(@Param("userId") UUID userId, @Param("limit") int limit);
}
