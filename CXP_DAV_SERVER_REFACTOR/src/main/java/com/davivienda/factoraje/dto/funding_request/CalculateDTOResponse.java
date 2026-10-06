package com.davivienda.factoraje.dto.funding_request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalculateDTOResponse {

    private BigDecimal amountToFinance;
    private BigDecimal amount;
    private BigDecimal interests;
    private BigDecimal commissions;
    private BigDecimal amountToBeDisbursed;
    private LocalDate disbursementDate;
    private List<DetailDTOResponse> detail = new ArrayList<>();
}
