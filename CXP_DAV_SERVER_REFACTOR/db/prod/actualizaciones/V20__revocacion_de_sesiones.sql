-- =============================================================================
-- ACTUALIZACIÓN V20 — Revocación de sesiones al cerrar sesión
-- =============================================================================
-- Equivale a src/main/resources/db/migration/V20__revocacion_de_sesiones.sql.
-- Agrega users.sessions_revoked_at: al cerrar sesión se guarda el momento y la API
-- rechaza los JWT emitidos antes, aunque no hayan vencido.
--
-- Solo aplica a bases en la versión V19; las instaladas con la versión actual de
-- instalacion_inicial.sql ya tienen la columna.
--
-- Uso:
--   psql -h <host> -U <usuario> -d <base> -f V20__revocacion_de_sesiones.sql
-- =============================================================================

\set ON_ERROR_STOP on

BEGIN;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'users'
                 AND column_name = 'sessions_revoked_at') THEN
        RAISE EXCEPTION 'La actualización V20 ya está aplicada: users.sessions_revoked_at ya existe.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_indexes
                   WHERE schemaname = 'public' AND indexname = 'uq_documents_generation_code'
                     AND indexdef LIKE '%INACTIVATED_BY_PAYER%')
       OR EXISTS (SELECT 1 FROM public.system_parameters
                  WHERE param_key = 'JWT_SECRET'
                    AND param_value = 'G9UuPSmEz8a18PARiE8Rs9QKvDrdUOJjjNe3duoQqUw=') THEN
        RAISE EXCEPTION 'La base no está en la versión V19; aplique antes las actualizaciones anteriores.';
    END IF;
END $$;

ALTER TABLE public.users ADD COLUMN sessions_revoked_at timestamp(6) with time zone;

COMMIT;
