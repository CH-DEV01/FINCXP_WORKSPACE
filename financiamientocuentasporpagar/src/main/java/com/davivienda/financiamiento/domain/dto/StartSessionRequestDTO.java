package com.davivienda.financiamiento.domain.dto;

public class StartSessionRequestDTO {

    private String sessionToken;

    public StartSessionRequestDTO() {}

    public StartSessionRequestDTO(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }
}
