-- Bitácora de documentos por pagador para el operador bancario (ADMIN). La ruta usa el mismo
-- path que la bitácora del pagador (documents-history) porque el navbar muestra el botón
-- BITACORA junto a INICIO cuando el rol tiene esa ruta. El menú va justo después de Inicio.

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
