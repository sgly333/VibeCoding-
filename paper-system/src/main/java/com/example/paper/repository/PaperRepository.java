package com.example.paper.repository;

import com.example.paper.entity.Paper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaperRepository extends JpaRepository<Paper, Long> {

    @Query("""
            SELECT DISTINCT p
            FROM Paper p
            LEFT JOIN FETCH p.paperCategories pc
            LEFT JOIN FETCH pc.category c
            WHERE (:keyword IS NULL OR p.title LIKE CONCAT('%', :keyword, '%'))
              AND (:categoryName IS NULL OR c.name = :categoryName)
            """)
    List<Paper> search(
            @Param("keyword") String keyword,
            @Param("categoryName") String categoryName
    );

    @Query("""
            SELECT DISTINCT p
            FROM Paper p
            LEFT JOIN FETCH p.paperCategories pc
            LEFT JOIN FETCH pc.category c
            WHERE p.id = :id
            """)
    Optional<Paper> findByIdWithCategories(@Param("id") Long id);
}

