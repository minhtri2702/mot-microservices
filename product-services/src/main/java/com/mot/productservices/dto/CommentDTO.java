package com.mot.productservices.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentDTO {
    private String id;
    private String mangaId;
    private Integer chapterId;
    private String userId;
    private String username;
    private String avatarUrl;
    private String parentCommentId;
    private String content;
    private int likeCount;
    private int replyCount;
    private boolean isLiked;
    private String createdAt;
    private String updatedAt;
    private List<CommentDTO> replies;
}
