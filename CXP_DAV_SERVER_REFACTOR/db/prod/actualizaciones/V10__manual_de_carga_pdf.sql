-- =============================================================================
-- ACTUALIZACIÓN V10 — Manual de la plantilla de carga en PDF
-- =============================================================================
-- Equivale a src/main/resources/db/migration/V10__manual_de_carga_pdf.sql. El manual
-- deja de ser texto Markdown y pasa a ser un PDF que publica el ADMIN, igual que la
-- plantilla. Elimina el manual en Markdown; hasta que se publique el PDF, las
-- pantallas de carga lo muestran como "No disponible".
--
-- Solo aplica a bases en la versión V9; las instaladas con la versión actual de
-- instalacion_inicial.sql ya lo incluyen.
--
-- Uso:
--   psql -h <host> -U <usuario> -d <base> -f V10__manual_de_carga_pdf.sql
--
-- Después, el ADMIN publica el manual (db/prod/manual_carga_documentos.pdf) desde
-- Recursos de carga.
-- =============================================================================

\set ON_ERROR_STOP on

BEGIN;

DO $$
BEGIN
    IF to_regclass('public.upload_resources') IS NULL THEN
        RAISE EXCEPTION 'La base no está en la versión V9; aplique antes las actualizaciones anteriores.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'public' AND table_name = 'upload_resources' AND column_name = 'content') THEN
        RAISE EXCEPTION 'La actualización V10 ya está aplicada: upload_resources no tiene la columna content.';
    END IF;
END $$;

DELETE FROM public.upload_resources WHERE resource_type = 'MANUAL';

ALTER TABLE public.upload_resources DROP CONSTRAINT upload_resources_content_check;
ALTER TABLE public.upload_resources DROP COLUMN content;

ALTER TABLE public.upload_resources
    ALTER COLUMN file_name SET NOT NULL,
    ALTER COLUMN file_content SET NOT NULL,
    ALTER COLUMN file_size SET NOT NULL,
    ADD CONSTRAINT upload_resources_file_size_check CHECK (file_size > 0);

COMMIT;
