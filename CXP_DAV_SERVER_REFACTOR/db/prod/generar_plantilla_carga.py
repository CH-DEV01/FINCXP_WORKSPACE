"""
Genera db/prod/plantilla_carga_documentos.xlsx, la plantilla inicial que el ADMIN publica
en Recursos de carga. Los encabezados son las columnas activas de excel_template_columns
(migraciones V2 y V6); si el catálogo cambia, hay que actualizar COLUMNS y volver a generarla,
porque la API rechaza una plantilla cuyos encabezados no coincidan con el catálogo.

Las celdas de cada columna quedan con el formato que exige la carga (Fecha, Número o Texto)
y las columnas con valores fijos tienen una lista desplegable. No usa macros.

Uso:
    python3 db/prod/generar_plantilla_carga.py [ruta_salida.xlsx]
"""

import sys
from pathlib import Path

from openpyxl import Workbook
from openpyxl.styles import Alignment, Font, NamedStyle, PatternFill
from openpyxl.utils import get_column_letter
from openpyxl.worksheet.datavalidation import DataValidation

# (encabezado, formato de celda, valores permitidos, ancho)
COLUMNS = [
    ("Fecha Emision", "DD/MM/YYYY", None, 15),
    ("Monto", "#,##0.00", None, 14),
    ("Numero Documento", "@", None, 22),
    ("Codigo Generacion", "@", None, 40),
    ("Sello Recepcion", "@", None, 44),
    ("Numero Control", "@", None, 34),
    ("Metodo Emision", "@", ["DIGITAL", "PAPER"], 16),
    ("Tipo Factura", "@", ["CCF", "FCI"], 13),
    ("NIT Proveedor", "@", None, 20),
    ("Nombre Proveedor", "@", None, 40),
    ("Politica Pago", "@", None, 14),
    ("Dia Desembolso", "@", None, 16),
    ("Cuenta Bancaria", "@", None, 20),
]

# Filas con formato y lista desplegable; Excel aplica el formato de columna a las demás.
FORMATTED_ROWS = 1000


def build(output: Path) -> None:
    wb = Workbook()
    ws = wb.active
    ws.title = "Documentos"

    header_font = Font(bold=True, color="FFFFFF")
    header_fill = PatternFill("solid", fgColor="C8102E")

    for index, (header, number_format, allowed, width) in enumerate(COLUMNS, start=1):
        letter = get_column_letter(index)
        cell = ws.cell(row=1, column=index, value=header)
        cell.font = header_font
        cell.fill = header_fill
        cell.alignment = Alignment(horizontal="center", vertical="center")

        style = NamedStyle(name=f"columna_{index}", number_format=number_format)
        wb.add_named_style(style)
        ws.column_dimensions[letter].width = width
        for row in range(2, FORMATTED_ROWS + 2):
            ws.cell(row=row, column=index).style = style.name

        if allowed:
            validation = DataValidation(
                type="list",
                formula1='"' + ",".join(allowed) + '"',
                allow_blank=True,
                showErrorMessage=True,
                errorTitle=header,
                error="Valores permitidos: " + ", ".join(allowed),
            )
            validation.add(f"{letter}2:{letter}{FORMATTED_ROWS + 1}")
            ws.add_data_validation(validation)

    ws.freeze_panes = "A2"

    output.parent.mkdir(parents=True, exist_ok=True)
    wb.save(output)


def main() -> None:
    default = Path(__file__).parent / "plantilla_carga_documentos.xlsx"
    output = Path(sys.argv[1]) if len(sys.argv) > 1 else default
    build(output)
    print(f"Plantilla: {output} ({output.stat().st_size} bytes, {len(COLUMNS)} columnas)")


if __name__ == "__main__":
    main()
