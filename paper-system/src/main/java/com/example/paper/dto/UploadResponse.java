package com.example.paper.dto;

import java.util.List;

public class UploadResponse {
    private Long paperId;
    private List<String> categories;

    public UploadResponse() {
    }

    public UploadResponse(Long paperId, List<String> categories) {
        this.paperId = paperId;
        this.categories = categories;
    }

    public Long getPaperId() {
        return paperId;
    }

    public void setPaperId(Long paperId) {
        this.paperId = paperId;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }
}

