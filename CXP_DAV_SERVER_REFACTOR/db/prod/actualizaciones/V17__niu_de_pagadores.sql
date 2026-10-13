-- =============================================================================
-- ACTUALIZACIÓN V17 — NIU de pagadores
-- =============================================================================
-- Equivale a src/main/resources/db/migration/V17__niu_de_pagadores.sql.
-- Pagadores y proveedores se registran con su NIU, único entre entidades. Las
-- entidades ya registradas y los proveedores creados desde la carga quedan sin NIU
-- hasta que se capture al editarlos.
--
-- Solo aplica a bases en la versión V16; las instaladas con la versión actual de
-- instalacion_inicial.sql ya lo incluyen.
--
-- Uso:
--   psql -h <host> -U <usuario> -d <base> -f V17__niu_de_pagadores.sql
-- =============================================================================

\set ON_ERROR_STOP on

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'public' AND table_name = 'credit_facility_histories'
                     AND column_name = 'disbursement_batch_id') THEN
        RAISE EXCEPTION 'La base no está en la versión V16; aplique antes las actualizaciones anteriores.';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'entities' AND column_name = 'niu') THEN
        RAISE EXCEPTION 'La actualización V17 ya está aplicada: entities ya tiene niu.';
    END IF;
END $$;

ALTER TABLE public.entities ADD COLUMN niu character varying(25);
ALTER TABLE public.entities ADD CONSTRAINT uk_entities_niu UNIQUE (niu);

COMMIT;
