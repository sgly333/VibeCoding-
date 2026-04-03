package com.example.paper.repository;

import com.example.paper.entity.PaperCategory;
import com.example.paper.entity.PaperCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaperCategoryRepository extends JpaRepository<PaperCategory, PaperCategoryId> {
    long countByIdCategoryId(Integer categoryId);

    @Modifying
    @Query("DELETE FROM PaperCategory pc WHERE pc.id.paperId = :paperId")
    int deleteAllByPaperId(@Param("paperId") Long paperId);
}

