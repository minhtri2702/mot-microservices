package com.mot.productservices.entity;

import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserChapterStatusId implements Serializable {
    private UUID userId;
    private Integer chapterId;
}
