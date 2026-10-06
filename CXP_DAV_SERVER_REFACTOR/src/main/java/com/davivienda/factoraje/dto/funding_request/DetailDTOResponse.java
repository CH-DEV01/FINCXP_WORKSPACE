package com.davivienda.factoraje.dto.funding_request;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DetailDTOResponse {

    private LocalDate issueDate; // enviar
    private LocalDate dueDate;
    private Integer financingDays; // enviar
    private String documentNumber;
    private BigDecimal amount;
    private BigDecimal amountToFinance;
    private BigDecimal interests;
    private BigDecimal commissions;
    private BigDecimal amountToBeDisbursed;
}
