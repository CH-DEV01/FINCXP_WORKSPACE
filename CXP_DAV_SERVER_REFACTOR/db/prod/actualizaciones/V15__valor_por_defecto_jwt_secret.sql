-- =============================================================================
-- ACTUALIZACIÓN V15 — Valor por defecto de JWT_SECRET
-- =============================================================================
-- Equivale a src/main/resources/db/migration/V15__valor_por_defecto_jwt_secret.sql.
-- Sustituye CONFIGURAR por la llave por defecto. Si el ambiente ya colocó otra
-- llave, el script no la cambia.
--
-- Solo aplica a bases en la versión V14; las instaladas con la versión actual de
-- instalacion_inicial.sql ya incluyen el valor por defecto.
--
-- Uso:
--   psql -h <host> -U <usuario> -d <base> -f V15__valor_por_defecto_jwt_secret.sql
-- =============================================================================

\set ON_ERROR_STOP on

BEGIN;

DO $$
DECLARE
    valor text;
BEGIN
    SELECT param_value INTO valor
    FROM public.system_parameters
    WHERE param_key = 'JWT_SECRET';

    IF valor IS NULL THEN
        RAISE EXCEPTION 'La base no está en la versión V14; aplique antes las actualizaciones anteriores.';
    END IF;
    IF valor = 'G9UuPSmEz8a18PARiE8Rs9QKvDrdUOJjjNe3duoQqUw=' THEN
        RAISE EXCEPTION 'La actualización V15 ya está aplicada: JWT_SECRET ya tiene el valor por defecto.';
    END IF;
END $$;

UPDATE public.system_parameters
SET param_value = 'G9UuPSmEz8a18PARiE8Rs9QKvDrdUOJjjNe3duoQqUw=',
    updated_at = now()
WHERE param_key = 'JWT_SECRET'
  AND param_value = 'CONFIGURAR';

COMMIT;
