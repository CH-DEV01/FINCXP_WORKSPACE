"""
Genera un Excel de carga para probar la agrupación de solicitudes de la terminal
de desembolsos, cuya clave es (fecha de solicitud, fecha de desembolso, vencimiento).

Usa dos proveedores del mismo pagador cuyos convenios ya existen:
  - Distribuidora Centroamericana (operaria Ana Lucía, convenio P30 / ONLY_FRIDAYS)
  - Comercializadora Salvadoreña (operario Luis, convenio P60 / T_PLUS_1)

La política del Excel solo se aplica al crear el convenio, así que las columnas
de política repiten las de los convenios existentes. La carga ya no crea usuarios:
Ana y Luis deben existir con rol SUPPLIER (se dan de alta en Gestión de usuarios).

Cada documento está asignado a un momento de solicitud:
  - R1: hoy antes de la hora de corte
  - R2: hoy después de la hora de corte
  - R3: el siguiente día hábil antes de la hora de corte
El combinar momentos y vencimientos produce solicitudes que se juntan (mismos
tres datos, distintos proveedores) y otras que se separan aunque compartan dos
de los tres datos. La hoja "Casos de prueba" indica qué documento solicitar, con
qué usuario y cuándo; "Solicitudes esperadas" muestra cómo debe verse la terminal.

Las fechas esperadas replican DisbursementPolicyServiceImpl sin feriados.

Uso:
    python3 generate_grouping_test_upload.py [ruta_salida.xlsx]
"""

import random
import sys
import uuid
from collections import OrderedDict
from datetime import date, datetime, timedelta
from pathlib import Path

from openpyxl import Workbook
from openpyxl.styles import Alignment, Font, PatternFill
from openpyxl.utils import get_column_letter

sys.path.insert(0, str(Path(__file__).parent))

from generate_sample_upload import HEADERS, POLICY_DAYS, control_number, random_stamp  # noqa: E402

CUTOFF = "15:00"
GRACE_DAYS = 5

ANA = {
    "key": "ANA",
    "user": "Ana Lucía Martínez López (DUI 04567890-1)",
    "nit": "0614-010101-101-1",
    "name": "Distribuidora Centroamericana S.A. de C.V.",
    "policy": "P30",
    "disbursement": "ONLY_FRIDAYS",
    "account": "100200300401",
}

LUIS = {
    "key": "LUIS",
    "user": "Luis Alberto Ramírez Castro (DUI 04567890-4)",
    "nit": "0614-040404-104-4",
    "name": "Comercializadora Salvadorena S.A. de C.V.",
    "policy": "P60",
    "disbursement": "T_PLUS_1",
    "account": "100200300404",
}

MOMENTS = {
    "R1": "Hoy antes de las 3:00 p. m.",
    "R2": "Hoy después de las 3:00 p. m.",
    "R3": "Siguiente día hábil antes de las 3:00 p. m.",
}

# (caso, proveedor, momento, vencimiento, objetivo)
CASES = [
    ("C1", LUIS, "R1", "D1", "Se junta con Ana en la misma solicitud (mismos tres datos)"),
    ("C1", LUIS, "R1", "D1", "Segundo documento de Luis en la misma solicitud"),
    ("C1", ANA, "R1", "D1", "Se junta con Luis: ambos desembolsan el mismo viernes"),
    ("C2", LUIS, "R1", "D2", "Mismo momento que C1 pero otro vencimiento: solicitud aparte"),
    ("C2", ANA, "R1", "D2", "Se junta con Luis en la solicitud del segundo vencimiento"),
    ("C3", ANA, "R1", "D3", "Solicitud de un solo proveedor"),
    ("C4", LUIS, "R2", "D1", "Misma fecha de solicitud y vencimiento que C1, pero el desembolso cambia por la hora de corte"),
    ("C5", ANA, "R2", "D1", "Igual que C4 para Ana: su desembolso pasa al viernes siguiente"),
    ("C6", LUIS, "R3", "D1", "Mismo desembolso y vencimiento que C4, pero otra fecha de solicitud: solicitud aparte"),
    ("C7", ANA, "R3", "D1", "Mismo desembolso y vencimiento que C5, pero otra fecha de solicitud: solicitud aparte"),
]


def is_business_day(day: date) -> bool:
    return day.weekday() < 5


def next_business_day_inclusive(day: date) -> date:
    while not is_business_day(day):
        day += timedelta(days=1)
    return day


def add_business_days(day: date, count: int) -> date:
    for _ in range(count):
        day = next_business_day_inclusive(day + timedelta(days=1))
    return day


def disbursement_date(effective_request: date, policy: str) -> date:
    earliest = add_business_days(next_business_day_inclusive(effective_request), 1)
    if policy == "T_PLUS_1":
        return earliest
    while earliest.weekday() != 4:  # ONLY_FRIDAYS
        earliest += timedelta(days=1)
    return earliest


def moment_dates(today: date):
    """Fecha de solicitud registrada y fecha efectiva (tras la hora de corte) de cada momento."""
    next_day = next_business_day_inclusive(today + timedelta(days=1))
    return {
        "R1": (today, today),
        "R2": (today, today + timedelta(days=1)),
        "R3": (next_day, next_day),
    }


def due_dates(today: date, latest_disbursement: date):
    # Financiable si vence después de desembolso + gracia; Ana (P30) necesita emisión <= hoy.
    first = latest_disbursement + timedelta(days=GRACE_DAYS + 3)
    dues = {"D1": first, "D2": first + timedelta(days=3), "D3": first + timedelta(days=6)}
    limit = today + timedelta(days=POLICY_DAYS[ANA["policy"]])
    if dues["D3"] > limit:
        raise ValueError(f"Los vencimientos superan {limit:%d/%m/%Y}; ejecuta el script otro día de la semana.")
    return dues


def build(today: date):
    moments = moment_dates(today)
    latest = max(
        disbursement_date(effective, supplier["disbursement"])
        for _, effective in moments.values()
        for supplier in (ANA, LUIS)
    )
    dues = due_dates(today, latest)

    run_tag = datetime.now().strftime("%m%d%H%M%S")
    seq_base = int(datetime.now().strftime("%y%m%d%H%M%S")) * 100
    rows = []
    counters = {"ANA": 0, "LUIS": 0}

    for seq, (case, supplier, moment, due_key, goal) in enumerate(CASES, start=1):
        counters[supplier["key"]] += 1
        due = dues[due_key]
        issue = due - timedelta(days=POLICY_DAYS[supplier["policy"]])
        request_date, effective = moments[moment]
        disbursement = disbursement_date(effective, supplier["disbursement"])
        digital = seq % 3 != 0
        invoice_type = "CCF" if seq % 2 else "FCI"
        prefix = "A" if supplier is ANA else "L"

        rows.append({
            "Fecha Emision": issue,
            "Monto": round(random.uniform(800, 4800), 2),
            "Numero Documento": f"{'DTE' if digital else 'FAC'}-{prefix}{run_tag}-{counters[supplier['key']]:02d}",
            "Codigo Generacion": str(uuid.uuid4()).upper() if digital else None,
            "Sello Recepcion": random_stamp() if digital else None,
            "Numero Control": control_number(invoice_type, seq_base + seq) if digital else None,
            "Metodo Emision": "DIGITAL" if digital else "PAPER",
            "Tipo Factura": invoice_type,
            "NIT Proveedor": supplier["nit"],
            "Nombre Proveedor": supplier["name"],
            "Politica Pago": supplier["policy"],
            "Dia Desembolso": supplier["disbursement"],
            "Cuenta Bancaria": supplier["account"],
            "_case": case,
            "_user": supplier["user"],
            "_moment": moment,
            "_request": request_date,
            "_disbursement": disbursement,
            "_due": due,
            "_goal": goal,
        })
    return rows


def expected_requests(rows):
    groups = OrderedDict()
    for row in rows:
        key = (row["_request"], row["_disbursement"], row["_due"])
        groups.setdefault(key, []).append(row)
    return groups


def style_header(row_cells):
    for cell in row_cells:
        cell.font = Font(bold=True, color="FFFFFF")
        cell.fill = PatternFill("solid", fgColor="C8102E")
        cell.alignment = Alignment(horizontal="center", vertical="center", wrap_text=True)


def write_workbook(rows, output: Path, today: date):
    wb = Workbook()
    ws = wb.active
    ws.title = "Documentos"
    ws.append(HEADERS)
    style_header(ws[1])
    for row in rows:
        ws.append([row[h] for h in HEADERS])
        r = ws.max_row
        ws.cell(row=r, column=1).number_format = "DD/MM/YYYY"
        ws.cell(row=r, column=2).number_format = "#,##0.00"
        for col_name in ("Cuenta Bancaria", "NIT Proveedor"):
            ws.cell(row=r, column=HEADERS.index(col_name) + 1).number_format = "@"
    for idx, header in enumerate(HEADERS, start=1):
        width = max(len(header), *(len(str(r[header] or "")) for r in rows)) + 2
        ws.column_dimensions[get_column_letter(idx)].width = min(width, 45)
    ws.freeze_panes = "A2"

    groups = expected_requests(rows)
    group_ids = {key: f"S{i}" for i, key in enumerate(groups, start=1)}

    cases = wb.create_sheet("Casos de prueba")
    cases.append([f"Generado el {today:%d/%m/%Y}. Hora de corte: {CUTOFF}. Fechas calculadas sin feriados."])
    cases.append([])
    cases.append(["Caso", "Numero Documento", "Usuario que solicita", "Proveedor", "Cuándo solicitar",
                  "Monto", "Vencimiento", "Fecha de solicitud esperada", "Fecha de desembolso esperada",
                  "Solicitud esperada", "Objetivo"])
    style_header(cases[3])
    for row in rows:
        key = (row["_request"], row["_disbursement"], row["_due"])
        cases.append([
            row["_case"], row["Numero Documento"], row["_user"], row["Nombre Proveedor"],
            f"{MOMENTS[row['_moment']]} ({row['_request']:%d/%m/%Y})", row["Monto"], row["_due"],
            row["_request"], row["_disbursement"], group_ids[key], row["_goal"],
        ])
        r = cases.max_row
        cases.cell(row=r, column=6).number_format = "#,##0.00"
        for col in (7, 8, 9):
            cases.cell(row=r, column=col).number_format = "DD/MM/YYYY"
    for col, width in zip("ABCDEFGHIJK", (7, 24, 40, 42, 44, 12, 13, 16, 16, 11, 90)):
        cases.column_dimensions[col].width = width
    cases.freeze_panes = "A4"

    summary = wb.create_sheet("Solicitudes esperadas")
    summary.append(["Así deben verse las solicitudes del pagador en la terminal después de hacer todas las solicitudes."])
    summary.append([])
    summary.append(["Solicitud", "Solicitado por el cliente", "Desembolso", "Vencimiento", "Proveedores",
                    "Documentos", "Monto nominal", "Detalle"])
    style_header(summary[3])
    for key, members in groups.items():
        request, disbursement, due = key
        suppliers = sorted({m["Nombre Proveedor"] for m in members})
        summary.append([
            group_ids[key], request, disbursement, due, len(suppliers), len(members),
            round(sum(m["Monto"] for m in members), 2),
            "; ".join(f"{m['Numero Documento']} ({m['Nombre Proveedor'].split()[0]})" for m in members),
        ])
        r = summary.max_row
        for col in (2, 3, 4):
            summary.cell(row=r, column=col).number_format = "DD/MM/YYYY"
        summary.cell(row=r, column=7).number_format = "#,##0.00"
    for col, width in zip("ABCDEFGH", (10, 18, 13, 13, 12, 12, 15, 110)):
        summary.column_dimensions[col].width = width

    steps = wb.create_sheet("Pasos")
    for line in [
        "1. Cargar la hoja Documentos con el usuario del pagador (Empresa Pagadora Demo).",
        "2. Hoy antes de las 3:00 p. m.: Luis solicita sus documentos de C1 y C2, y Ana los de C1, C2 y C3.",
        "   Pueden ir en una sola solicitud por usuario: la terminal separa por vencimiento.",
        "3. Hoy después de las 3:00 p. m.: Luis solicita el documento de C4 y Ana el de C5.",
        "4. El siguiente día hábil antes de las 3:00 p. m.: Luis solicita el documento de C6 y Ana el de C7.",
        "5. En la terminal del operador bancario, comparar las solicitudes del pagador con la hoja Solicitudes esperadas.",
        "6. Generar el lote de S1 y revisar que la carta incluya una fila por proveedor (Luis y Ana).",
        "",
        "Si no se quiere esperar a la hora de corte, se puede cambiar el parámetro DISBURSEMENT_CUTOFF_TIME",
        "(por ejemplo a la hora actual) antes de las solicitudes R2 y regresarlo a 15:00 después.",
    ]:
        steps.append([line])
    steps.column_dimensions["A"].width = 120

    output.parent.mkdir(parents=True, exist_ok=True)
    wb.save(output)
    return groups, group_ids


def main():
    today = date.today()
    default_name = f"carga_prueba_agrupacion_{today:%Y%m%d}.xlsx"
    output = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).parent / default_name

    rows = build(today)
    groups, group_ids = write_workbook(rows, output, today)

    print(f"Archivo: {output}")
    print(f"Documentos: {len(rows)}  |  Monto total: ${sum(r['Monto'] for r in rows):,.2f}")
    for key, members in groups.items():
        request, disbursement, due = key
        suppliers = sorted({m["Nombre Proveedor"].split()[0] for m in members})
        print(f"  {group_ids[key]}: solicitud {request:%d/%m} | desembolso {disbursement:%d/%m} | "
              f"vence {due:%d/%m} | {len(members)} doc(s) | {', '.join(suppliers)}")


if __name__ == "__main__":
    main()
