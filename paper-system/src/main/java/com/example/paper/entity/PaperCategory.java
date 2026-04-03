package com.example.paper.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "paper_category")
public class PaperCategory {

    @EmbeddedId
    private PaperCategoryId id = new PaperCategoryId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("paperId")
    @JoinColumn(name = "paper_id", nullable = false)
    private Paper paper;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("categoryId")
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    public PaperCategory() {
    }

    public PaperCategory(Paper paper, Category category) {
        this.paper = paper;
        this.category = category;
        this.id = new PaperCategoryId(paper.getId(), category.getId());
    }

    public PaperCategoryId getId() {
        return id;
    }

    public void setId(PaperCategoryId id) {
        this.id = id;
    }

    public Paper getPaper() {
        return paper;
    }

    public void setPaper(Paper paper) {
        this.paper = paper;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}

