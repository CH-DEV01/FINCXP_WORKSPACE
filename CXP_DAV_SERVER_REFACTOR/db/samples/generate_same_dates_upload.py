"""
Genera un Excel de carga con dos proveedores del mismo pagador que comparten
fechas de emisión, política de pago y política de desembolso. Sus documentos
tienen los mismos vencimientos, de modo que si ambos proveedores solicitan el
mismo día, sus documentos caen en la misma solicitud (vencimiento, solicitud,
desembolso) de la terminal y en el mismo lote.

Solo incluye documentos financiables.

La política del Excel solo se aplica al crear el convenio: si el convenio
pagador-proveedor ya existe, se conserva la suya. Por eso se usa el proveedor
demo del seed (convenio P60/T_PLUS_1) y un proveedor nuevo con P60/T_PLUS_1.

Uso:
    python3 generate_same_dates_upload.py [ruta_salida.xlsx]
"""

import random
import sys
import uuid
from datetime import date, datetime, timedelta
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from generate_sample_upload import (  # noqa: E402
    POLICY_DAYS,
    control_number,
    random_stamp,
    write_workbook,
)

POLICY = "P60"
DISBURSEMENT = "T_PLUS_1"

# Días hasta el vencimiento respecto a hoy, comunes a ambos proveedores.
DUE_OFFSETS = [10, 15, 22, 30, 40, 50]

SUPPLIERS = [
    {
        "nit": "0614-000000-003-2",
        "name": "Proveedor Demo El Salvador S.A. de C.V.",
        "account": "000987654321",
    },
    {
        "nit": "0614-040404-104-4",
        "name": "Comercializadora Salvadorena S.A. de C.V.",
        "account": "100200300404",
    },
]


def build_rows(today: date):
    run_tag = datetime.now().strftime("%m%d%H%M%S")
    seq_base = int(datetime.now().strftime("%y%m%d%H%M%S")) * 100
    days = POLICY_DAYS[POLICY]
    rows = []
    seq = 0

    for d_index, offset in enumerate(DUE_OFFSETS):
        due = today + timedelta(days=offset)
        issue = due - timedelta(days=days)
        if issue > today or issue < today - timedelta(days=120):
            raise ValueError(f"Fecha de emisión fuera de rango (offset {offset})")

        for s_index, supplier in enumerate(SUPPLIERS):
            seq += 1
            digital = (d_index % 3) != 2
            invoice_type = "CCF" if (d_index % 2 == 0) else "FCI"

            rows.append({
                "Fecha Emision": issue,
                "Monto": round(random.uniform(350, 4800), 2),
                "Numero Documento": f"{'DTE' if digital else 'FAC'}-{s_index + 5}{run_tag}-{d_index + 1:02d}",
                "Codigo Generacion": str(uuid.uuid4()).upper() if digital else None,
                "Sello Recepcion": random_stamp() if digital else None,
                "Numero Control": control_number(invoice_type, seq_base + seq) if digital else None,
                "Metodo Emision": "DIGITAL" if digital else "PAPER",
                "Tipo Factura": invoice_type,
                "NIT Proveedor": supplier["nit"],
                "Nombre Proveedor": supplier["name"],
                "Politica Pago": POLICY,
                "Dia Desembolso": DISBURSEMENT,
                "Cuenta Bancaria": supplier["account"],
                "_due": due,
                "_offset": offset,
            })
    return rows


def main():
    today = date.today()
    default_name = f"carga_mismas_fechas_{today.strftime('%Y%m%d')}.xlsx"
    output = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).parent / default_name

    rows = build_rows(today)
    write_workbook(rows, output, today)

    total = sum(r["Monto"] for r in rows)
    print(f"Archivo: {output}")
    print(f"Documentos: {len(rows)}  |  Proveedores: {len(SUPPLIERS)}  |  Monto total: ${total:,.2f}")
    for offset in DUE_OFFSETS:
        sample = next(r for r in rows if r["_offset"] == offset)
        print(f"  Emisión {sample['Fecha Emision']:%d/%m/%Y} -> vencimiento {sample['_due']:%d/%m/%Y}")


if __name__ == "__main__":
    main()
