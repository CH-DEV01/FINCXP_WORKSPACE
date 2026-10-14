-- =============================================================================
-- ACTUALIZACIÓN V21 — Sesiones pendientes del SSO
-- =============================================================================
-- Equivale a src/main/resources/db/migration/V21__sesiones_pendientes_sso.sql.
-- Crea sso_pending_sessions: el handoff del SSO guarda ahí un token de un solo uso
-- (vence a los 5 minutos) que el portal canjea por la sesión del usuario.
--
-- Solo aplica a bases en la versión V20; las instaladas con la versión actual de
-- instalacion_inicial.sql ya tienen la tabla.
--
-- Uso:
--   psql -h <host> -U <usuario> -d <base> -f V21__sesiones_pendientes_sso.sql
-- =============================================================================

\set ON_ERROR_STOP on

BEGIN;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = 'public' AND table_name = 'sso_pending_sessions') THEN
        RAISE EXCEPTION 'La actualización V21 ya está aplicada: sso_pending_sessions ya existe.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'public' AND table_name = 'users'
                     AND column_name = 'sessions_revoked_at') THEN
        RAISE EXCEPTION 'La base no está en la versión V20; aplique antes las actualizaciones anteriores.';
    END IF;
END $$;

CREATE TABLE public.sso_pending_sessions (
    session_token character varying(255) NOT NULL,
    user_email character varying(255) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT sso_pending_sessions_pkey PRIMARY KEY (session_token)
);

COMMIT;
