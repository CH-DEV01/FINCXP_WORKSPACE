package com.davivienda.factoraje.infrastructure.file_parser.excel;

@FunctionalInterface
public interface ExcelRowMapper<T> {
    /**
     * Mapea una fila física de Excel a un Record (DTO) inmutable.
     * 
     * @param accessor Herramienta segura para extraer los datos de la fila.
     * @param rowIndex El número de fila visual en Excel (para reportar errores).
     * @return La instancia del DTO.
     */
    T mapRow(ExcelRowAccessor accessor, int rowIndex);
}
