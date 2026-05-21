package com.mot.productservices.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserChapterStatusId implements Serializable {
    private String userId;
    private Integer chapterId;
}
