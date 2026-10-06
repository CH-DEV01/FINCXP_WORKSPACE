-- =============================================================================
-- ACTUALIZACIÓN V11 — Bitácora de documentos del operador bancario
-- =============================================================================
-- Equivale a src/main/resources/db/migration/V11__bitacora_de_documentos_operador.sql.
-- Agrega al operador bancario (ADMIN) la bitácora de documentos por pagador: el botón
-- BITACORA junto a INICIO y su menú en la posición 2.
--
-- Solo aplica a bases en la versión V10; las instaladas con la versión actual de
-- instalacion_inicial.sql ya lo incluyen.
--
-- Uso:
--   psql -h <host> -U <usuario> -d <base> -f V11__bitacora_de_documentos_operador.sql
-- =============================================================================

\set ON_ERROR_STOP on

BEGIN;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'upload_resources' AND column_name = 'content') THEN
        RAISE EXCEPTION 'La base no está en la versión V10; aplique antes las actualizaciones anteriores.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.routes_cat WHERE id = 'cb000000-0000-4000-8000-000000000001') THEN
        RAISE EXCEPTION 'La actualización V11 ya está aplicada: la ruta OperatorDocumentLog ya existe.';
    END IF;
END $$;

INSERT INTO public.routes_cat (id, component_name, created_at, description, path, status, updated_at) VALUES
    ('cb000000-0000-4000-8000-000000000001', 'OperatorDocumentLog', now(), 'Bitácora de documentos (operador)', 'documents-history', 'ACTIVE', now());

INSERT INTO public.role_routes (id, created_at, is_index, role_id, route_id)
SELECT 'cb000000-0000-4000-8000-000000000011', now(), false, r.id, 'cb000000-0000-4000-8000-000000000001'
FROM public.roles_cat r
WHERE r.name = 'ADMIN';

INSERT INTO public.menus_cat (id, created_at, description, icon, label, path, status, updated_at) VALUES
    ('cb000000-0000-4000-8000-000000000021', now(), NULL, 'clock', 'Bitácora de documentos', '/admin/documents-history', 'ACTIVE', now());

UPDATE public.role_menus rm
SET display_order = rm.display_order + 1
FROM public.roles_cat r
WHERE rm.role_id = r.id AND r.name = 'ADMIN' AND rm.display_order >= 2;

INSERT INTO public.role_menus (id, created_at, display_order, menu_id, role_id)
SELECT 'cb000000-0000-4000-8000-000000000031', now(), 2, 'cb000000-0000-4000-8000-000000000021', r.id
FROM public.roles_cat r
WHERE r.name = 'ADMIN';

COMMIT;
