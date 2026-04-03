package com.example.paper.dto;

public class LoginResponse {
    private boolean ok;
    private String username;

    public LoginResponse() {
    }

    public LoginResponse(boolean ok, String username) {
        this.ok = ok;
        this.username = username;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}

