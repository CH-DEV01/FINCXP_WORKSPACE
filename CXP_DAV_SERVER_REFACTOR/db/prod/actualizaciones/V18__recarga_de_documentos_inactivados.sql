-- =============================================================================
-- ACTUALIZACIÓN V18 — Recarga de documentos inactivados por el pagador
-- =============================================================================
-- Equivale a src/main/resources/db/migration/V18__recarga_de_documentos_inactivados.sql.
-- Los índices de doble fondeo dejan de considerar los documentos inactivados por el
-- pagador, para que se puedan volver a cargar. Los documentos en cuarentena siguen
-- contando, porque aún pueden dispersarse.
--
-- Solo aplica a bases en la versión V17; las instaladas con la versión actual de
-- instalacion_inicial.sql ya lo incluyen.
--
-- Uso:
--   psql -h <host> -U <usuario> -d <base> -f V18__recarga_de_documentos_inactivados.sql
-- =============================================================================

\set ON_ERROR_STOP on

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'public' AND table_name = 'entities' AND column_name = 'niu') THEN
        RAISE EXCEPTION 'La base no está en la versión V17; aplique antes las actualizaciones anteriores.';
    END IF;
    IF EXISTS (SELECT 1 FROM pg_indexes
               WHERE schemaname = 'public' AND indexname = 'uq_documents_generation_code'
                 AND indexdef LIKE '%INACTIVATED_BY_PAYER%') THEN
        RAISE EXCEPTION 'La actualización V18 ya está aplicada: los índices de doble fondeo ya excluyen los documentos inactivados.';
    END IF;
END $$;

DROP INDEX public.uq_documents_control_number;
DROP INDEX public.uq_documents_generation_code;
DROP INDEX public.uq_documents_paper_number_year;
DROP INDEX public.uq_documents_received_stamp;

CREATE UNIQUE INDEX uq_documents_control_number ON public.documents USING btree (upper((control_number)::text))
    WHERE (control_number IS NOT NULL AND status <> 'INACTIVATED_BY_PAYER');
CREATE UNIQUE INDEX uq_documents_generation_code ON public.documents USING btree (upper((generation_code)::text))
    WHERE (generation_code IS NOT NULL AND status <> 'INACTIVATED_BY_PAYER');
CREATE UNIQUE INDEX uq_documents_paper_number_year ON public.documents USING btree
    (master_agreement_id, document_number, EXTRACT(year FROM issue_date))
    WHERE (issuance_method = 'PAPER' AND document_number IS NOT NULL AND status <> 'INACTIVATED_BY_PAYER');
CREATE UNIQUE INDEX uq_documents_received_stamp ON public.documents USING btree (upper((received_stamp)::text))
    WHERE (received_stamp IS NOT NULL AND status <> 'INACTIVATED_BY_PAYER');

COMMIT;
