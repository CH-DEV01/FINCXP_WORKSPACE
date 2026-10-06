-- =============================================================================
-- ACTUALIZACIÓN V14 — Secreto JWT
-- =============================================================================
-- Equivale a src/main/resources/db/migration/V14__secreto_jwt.sql.
-- Deja la llave de firma en CONFIGURAR. Antes de arrancar la API hay que
-- reemplazarla por una llave Base64 de al menos 32 bytes:
--   openssl rand -base64 32
-- En un ambiente que ya usaba la variable de entorno JWT_SECRET, copiar ese
-- mismo valor para no invalidar las sesiones emitidas.
--
-- Solo aplica a bases en la versión V13; las instaladas con la versión actual de
-- instalacion_inicial.sql ya lo incluyen.
--
-- Uso:
--   psql -h <host> -U <usuario> -d <base> -f V14__secreto_jwt.sql
-- =============================================================================

\set ON_ERROR_STOP on

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.system_parameters WHERE param_key = 'MAILJET_API_URL') THEN
        RAISE EXCEPTION 'La base no está en la versión V13; aplique antes las actualizaciones anteriores.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.system_parameters WHERE param_key = 'JWT_SECRET') THEN
        RAISE EXCEPTION 'La actualización V14 ya está aplicada: el parámetro JWT_SECRET ya existe.';
    END IF;
END $$;

INSERT INTO public.system_parameters (id, created_at, param_key, updated_at, param_value) VALUES
    ('d1400000-0000-4000-8000-000000000001', now(), 'JWT_SECRET', now(), 'CONFIGURAR');

COMMIT;
