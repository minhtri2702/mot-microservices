package com.mot.productservices.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserChapterStatusId implements Serializable {
    private UUID userId;
    private Integer chapterId;
}
