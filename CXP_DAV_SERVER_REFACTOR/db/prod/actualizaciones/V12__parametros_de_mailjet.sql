-- =============================================================================
-- ACTUALIZACIÓN V12 — Parámetros de Mailjet
-- =============================================================================
-- Equivale a src/main/resources/db/migration/V12__parametros_de_mailjet.sql.
-- Deja las credenciales, el remitente y la URL de acceso en CONFIGURAR. El
-- administrador del sistema las reemplaza desde Gestión de parámetros.
--
-- Solo aplica a bases en la versión V11; las instaladas con la versión actual de
-- instalacion_inicial.sql ya lo incluyen.
--
-- Uso:
--   psql -h <host> -U <usuario> -d <base> -f V12__parametros_de_mailjet.sql
-- =============================================================================

\set ON_ERROR_STOP on

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.routes_cat WHERE id = 'cb000000-0000-4000-8000-000000000001') THEN
        RAISE EXCEPTION 'La base no está en la versión V11; aplique antes las actualizaciones anteriores.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.system_parameters WHERE param_key = 'MAILJET_API_KEY') THEN
        RAISE EXCEPTION 'La actualización V12 ya está aplicada: el parámetro MAILJET_API_KEY ya existe.';
    END IF;
END $$;

INSERT INTO public.system_parameters (id, created_at, param_key, updated_at, param_value) VALUES
    ('d1200000-0000-4000-8000-000000000001', now(), 'MAILJET_API_KEY', now(), 'CONFIGURAR'),
    ('d1200000-0000-4000-8000-000000000002', now(), 'MAILJET_API_SECRET', now(), 'CONFIGURAR'),
    ('d1200000-0000-4000-8000-000000000003', now(), 'MAILJET_FROM_EMAIL', now(), 'CONFIGURAR'),
    ('d1200000-0000-4000-8000-000000000004', now(), 'MAILJET_FROM_NAME', now(), 'CONFIGURAR'),
    ('d1200000-0000-4000-8000-000000000005', now(), 'APP_LOGIN_URL', now(), 'CONFIGURAR');

COMMIT;
