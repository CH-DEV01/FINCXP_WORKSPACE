-- =============================================================================
-- ACTUALIZACIÓN V13 — URL de Mailjet
-- =============================================================================
-- Equivale a src/main/resources/db/migration/V13__url_de_mailjet.sql.
-- Deja el endpoint que consume el envío de correos. El administrador del sistema
-- puede cambiarlo desde Gestión de parámetros.
--
-- Solo aplica a bases en la versión V12; las instaladas con la versión actual de
-- instalacion_inicial.sql ya lo incluyen.
--
-- Uso:
--   psql -h <host> -U <usuario> -d <base> -f V13__url_de_mailjet.sql
-- =============================================================================

\set ON_ERROR_STOP on

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.system_parameters WHERE param_key = 'MAILJET_API_KEY') THEN
        RAISE EXCEPTION 'La base no está en la versión V12; aplique antes las actualizaciones anteriores.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.system_parameters WHERE param_key = 'MAILJET_API_URL') THEN
        RAISE EXCEPTION 'La actualización V13 ya está aplicada: el parámetro MAILJET_API_URL ya existe.';
    END IF;
END $$;

INSERT INTO public.system_parameters (id, created_at, param_key, updated_at, param_value) VALUES
    ('d1300000-0000-4000-8000-000000000001', now(), 'MAILJET_API_URL', now(), 'https://api.mailjet.com/v3/send');

COMMIT;
