package com.example.paper.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PaperCategoryId implements Serializable {

    private Long paperId;
    private Integer categoryId;

    public PaperCategoryId() {
    }

    public PaperCategoryId(Long paperId, Integer categoryId) {
        this.paperId = paperId;
        this.categoryId = categoryId;
    }

    public Long getPaperId() {
        return paperId;
    }

    public void setPaperId(Long paperId) {
        this.paperId = paperId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaperCategoryId that)) return false;
        return Objects.equals(paperId, that.paperId) && Objects.equals(categoryId, that.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paperId, categoryId);
    }
}

