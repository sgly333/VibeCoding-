package com.example.paper.dto;

import java.util.List;

public class PaperUpdateRequest {

    private List<String> categories;

    private String codeUrl;

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public String getCodeUrl() {
        return codeUrl;
    }

    public void setCodeUrl(String codeUrl) {
        this.codeUrl = codeUrl;
    }
}

