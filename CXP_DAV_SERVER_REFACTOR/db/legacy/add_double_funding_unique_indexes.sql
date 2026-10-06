-- Índices únicos que respaldan en la base las validaciones de doble fondeo. Sin ellos, dos
-- cargas o solicitudes simultáneas pueden pasar la validación de la aplicación a la vez.
--   documents: código de generación, número de control y sello de recepción del DTE
--              (únicos en todo el país, sin distinguir mayúsculas)
--   documents: factura física (PAPER) por convenio, número de documento y año de emisión
--   financing_transactions: una sola transacción de financiamiento por documento
--
-- Ejecutar después de normalize_identifiers.sql. Si ya existen duplicados, aborta y los lista
-- sin crear ningún índice.
--
-- El índice de facturas físicas es por convenio, no por proveedor: si dos pagadores distintos
-- cargan a la vez el mismo número físico del mismo proveedor, solo lo detecta la validación
-- de la aplicación.

BEGIN;

DO $$
DECLARE
    conflicts TEXT;
BEGIN
    SELECT string_agg(detail, E'\n') INTO conflicts FROM (
        SELECT 'documents.generation_code ' || upper(generation_code) || ' (' || count(*) || ' veces)' AS detail
        FROM documents WHERE generation_code IS NOT NULL
        GROUP BY upper(generation_code) HAVING count(*) > 1
        UNION ALL
        SELECT 'documents.control_number ' || upper(control_number) || ' (' || count(*) || ' veces)'
        FROM documents WHERE control_number IS NOT NULL
        GROUP BY upper(control_number) HAVING count(*) > 1
        UNION ALL
        SELECT 'documents.received_stamp ' || upper(received_stamp) || ' (' || count(*) || ' veces)'
        FROM documents WHERE received_stamp IS NOT NULL
        GROUP BY upper(received_stamp) HAVING count(*) > 1
        UNION ALL
        SELECT 'documents físico ' || document_number || ' año ' || EXTRACT(YEAR FROM issue_date)
               || ' convenio ' || master_agreement_id || ' (' || count(*) || ' veces)'
        FROM documents WHERE issuance_method = 'PAPER' AND document_number IS NOT NULL
        GROUP BY master_agreement_id, document_number, EXTRACT(YEAR FROM issue_date) HAVING count(*) > 1
        UNION ALL
        SELECT 'financing_transactions.document_id ' || document_id || ' (' || count(*) || ' veces)'
        FROM financing_transactions
        GROUP BY document_id HAVING count(*) > 1
    ) c;

    IF conflicts IS NOT NULL THEN
        RAISE EXCEPTION E'Existen documentos duplicados. Resuélvalos antes de crear los índices:\n%', conflicts;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_documents_generation_code
    ON documents (upper(generation_code)) WHERE generation_code IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_documents_control_number
    ON documents (upper(control_number)) WHERE control_number IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_documents_received_stamp
    ON documents (upper(received_stamp)) WHERE received_stamp IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_documents_paper_number_year
    ON documents (master_agreement_id, document_number, (EXTRACT(YEAR FROM issue_date)))
    WHERE issuance_method = 'PAPER' AND document_number IS NOT NULL;

-- Hibernate crea esta restricción (unique = true) solo si generó la tabla.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_index i
        JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY (i.indkey)
        WHERE i.indrelid = 'financing_transactions'::regclass
          AND i.indisunique
          AND i.indnatts = 1
          AND a.attname = 'document_id'
    ) THEN
        CREATE UNIQUE INDEX uq_financing_transactions_document ON financing_transactions (document_id);
    END IF;
END $$;

COMMIT;
