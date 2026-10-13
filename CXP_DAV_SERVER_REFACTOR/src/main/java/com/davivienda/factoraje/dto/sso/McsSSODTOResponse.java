package com.davivienda.factoraje.dto.sso;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class McsSSODTOResponse {
    
    private String message;
    private Boolean success;
    @ToString.Exclude
    private String jwt;
}
