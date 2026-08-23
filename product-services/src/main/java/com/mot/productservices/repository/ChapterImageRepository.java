package com.mot.productservices.repository;

import com.mot.productservices.entity.ChapterImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChapterImageRepository extends JpaRepository<ChapterImage, Integer> {

    Optional<ChapterImage> findByChapterIdAndPageOrder(Integer chapterId, Integer pageOrder);

    void deleteByChapterIdAndPageOrder(Integer chapterId, Integer pageOrder);
}
