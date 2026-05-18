package com.mot.productservices.repository;

import com.mot.productservices.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Integer> {

    Optional<Genre> findBySlug(String slug);

    Optional<Genre> findByName(String name);
}
