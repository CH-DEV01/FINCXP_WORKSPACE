-- =============================================================================
-- ACTUALIZACIÓN V16 — Abonos al cupo por lote de desembolso
-- =============================================================================
-- Equivale a src/main/resources/db/migration/V16__abonos_por_lote_de_desembolso.sql.
-- El pago parcial del cupo se registra por lote de desembolso: el historial del cupo
-- guarda el lote abonado y cada lote se abona una sola vez.
--
-- Solo aplica a bases en la versión V15; las instaladas con la versión actual de
-- instalacion_inicial.sql ya lo incluyen.
--
-- Uso:
--   psql -h <host> -U <usuario> -d <base> -f V16__abonos_por_lote_de_desembolso.sql
-- =============================================================================

\set ON_ERROR_STOP on

BEGIN;

DO $$
BEGIN
    -- Se revisa antes que la versión previa: V19 devuelve JWT_SECRET a CONFIGURAR.
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'credit_facility_histories'
                 AND column_name = 'disbursement_batch_id') THEN
        RAISE EXCEPTION 'La actualización V16 ya está aplicada: credit_facility_histories ya tiene disbursement_batch_id.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM public.system_parameters
                   WHERE param_key = 'JWT_SECRET' AND param_value <> 'CONFIGURAR') THEN
        RAISE EXCEPTION 'La base no está en la versión V15; aplique antes las actualizaciones anteriores.';
    END IF;
END $$;

ALTER TABLE public.credit_facility_histories ADD COLUMN disbursement_batch_id uuid;
ALTER TABLE public.credit_facility_histories ADD CONSTRAINT fk_credit_facility_histories_disbursement_batch
    FOREIGN KEY (disbursement_batch_id) REFERENCES public.disbursement_batches(id);
CREATE UNIQUE INDEX uk_credit_facility_histories_disbursement_batch
    ON public.credit_facility_histories (disbursement_batch_id) WHERE disbursement_batch_id IS NOT NULL;

COMMIT;
