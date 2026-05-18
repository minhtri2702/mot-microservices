package com.mot.productservices.repository;

import com.mot.productservices.entity.ChapterImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChapterImageRepository extends JpaRepository<ChapterImage, Integer> {
}
