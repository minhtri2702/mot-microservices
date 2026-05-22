package com.mot.productservices.repository;

import com.mot.productservices.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    // Get root comments (parent_comment_id IS NULL) for a manga, ordered by newest first
    Page<Comment> findByMangaIdAndParentCommentIdIsNullAndIsDeletedFalseOrderByCreatedAtDesc(
            UUID mangaId, Pageable pageable);

    // Get replies for a specific comment
    List<Comment> findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID parentCommentId);

    // Count total root comments for a manga
    long countByMangaIdAndParentCommentIdIsNullAndIsDeletedFalse(UUID mangaId);

    // Increment reply count
    @Modifying
    @Query("UPDATE Comment c SET c.replyCount = c.replyCount + 1 WHERE c.id = :id")
    void incrementReplyCount(@Param("id") UUID id);

    // Increment like count
    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id")
    void incrementLikeCount(@Param("id") UUID id);

    // Decrement like count
    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = CASE WHEN c.likeCount > 0 THEN c.likeCount - 1 ELSE 0 END WHERE c.id = :id")
    void decrementLikeCount(@Param("id") UUID id);
}
