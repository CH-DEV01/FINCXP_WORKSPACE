package com.davivienda.factoraje.dto.sso;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HandoffDTOResponse {

    private Boolean success;
    private DataSession data;
}
