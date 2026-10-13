-- =============================================================================
-- ACTUALIZACIÓN V19 — JWT_SECRET sin valor por defecto
-- =============================================================================
-- Equivale a src/main/resources/db/migration/V19__jwt_secret_sin_valor_por_defecto.sql.
-- La llave por defecto que colocó V15 está en el código fuente, así que JWT_SECRET
-- vuelve a CONFIGURAR. Si el ambiente ya colocó otra llave, el script no la cambia.
--
-- Después de aplicarla, la API no arranca hasta que se coloque una llave propia del
-- ambiente (Base64 de al menos 32 bytes, generada con: openssl rand -base64 32):
--   UPDATE public.system_parameters SET param_value = '<llave>', updated_at = now()
--   WHERE param_key = 'JWT_SECRET';
-- Cambiar la llave invalida las sesiones abiertas.
--
-- Solo aplica a bases en la versión V18; las instaladas con la versión actual de
-- instalacion_inicial.sql ya dejan JWT_SECRET en CONFIGURAR.
--
-- Uso:
--   psql -h <host> -U <usuario> -d <base> -f V19__jwt_secret_sin_valor_por_defecto.sql
-- =============================================================================

\set ON_ERROR_STOP on

BEGIN;

DO $$
DECLARE
    valor text;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes
                   WHERE schemaname = 'public' AND indexname = 'uq_documents_generation_code'
                     AND indexdef LIKE '%INACTIVATED_BY_PAYER%') THEN
        RAISE EXCEPTION 'La base no está en la versión V18; aplique antes las actualizaciones anteriores.';
    END IF;

    SELECT param_value INTO valor
    FROM public.system_parameters
    WHERE param_key = 'JWT_SECRET';

    IF valor IS DISTINCT FROM 'G9UuPSmEz8a18PARiE8Rs9QKvDrdUOJjjNe3duoQqUw=' THEN
        RAISE EXCEPTION 'La actualización V19 ya está aplicada: JWT_SECRET ya no tiene la llave por defecto.';
    END IF;
END $$;

UPDATE public.system_parameters
SET param_value = 'CONFIGURAR',
    updated_at = now()
WHERE param_key = 'JWT_SECRET'
  AND param_value = 'G9UuPSmEz8a18PARiE8Rs9QKvDrdUOJjjNe3duoQqUw=';

COMMIT;
