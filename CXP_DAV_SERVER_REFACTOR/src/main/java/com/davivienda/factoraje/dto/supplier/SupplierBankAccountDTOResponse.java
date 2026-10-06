package com.davivienda.factoraje.dto.supplier;

/**
 * @param accountNumber cuenta principal del proveedor, a la que se abona el desembolso; nula si no
 *                      tiene.
 */
public record SupplierBankAccountDTOResponse(String accountNumber) {
}
