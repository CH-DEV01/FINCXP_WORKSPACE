package com.davivienda.factoraje.dto.sso;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class McsSSODTOResponse {
    
    private String message;
    private Boolean success;
    private String jwt;
}
