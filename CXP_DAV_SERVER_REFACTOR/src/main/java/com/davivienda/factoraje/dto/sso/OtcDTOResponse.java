package com.davivienda.factoraje.dto.sso;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtcDTOResponse {

    private Boolean success;
    private UserData data;
    private String message;
    private String timestamp;
    private String agent;
    
}
