package com.davivienda.financiamiento.domain.dto;

/** El JWT no viene en el cuerpo: la API lo envía en la cookie de sesión (Set-Cookie). */
public class StartSessionResponseDTO {

    private Boolean success;
    private String message;

    public StartSessionResponseDTO() {}

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
