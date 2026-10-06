-- Crea el rol SYSTEM_ADMIN, responsable de los parámetros del sistema, con su
-- ruta base /system y la pantalla de parámetros como única pantalla.
-- Prerrequisito: db/seed_routes_menus.sql (crea la ruta param-management).
-- Idempotente.
-- Uso:
--   psql -h localhost -U postgres -d postgres -f db/add_system_admin_role.sql

BEGIN;

INSERT INTO roles_cat (id, name, description, status, default_route, created_at, updated_at) VALUES
  ('b1000000-0000-4000-8000-000000000007', 'SYSTEM_ADMIN', 'Administrador de parámetros del sistema', 'ACTIVE', '/system', NOW(), NOW())
ON CONFLICT (name) DO UPDATE SET default_route = EXCLUDED.default_route, updated_at = NOW();

INSERT INTO routes_cat (id, path, component_name, description, status, created_at, updated_at) VALUES
  (gen_random_uuid(), '', 'ParamManagement', 'Gestión de parámetros (inicio)', 'ACTIVE', NOW(), NOW())
ON CONFLICT ON CONSTRAINT uk_routes_cat_path_component DO NOTHING;

INSERT INTO role_routes (id, role_id, route_id, is_index, created_at)
SELECT gen_random_uuid(), ro.id, rt.id, TRUE, NOW()
FROM roles_cat ro
JOIN routes_cat rt ON rt.path = '' AND rt.component_name = 'ParamManagement'
WHERE ro.name = 'SYSTEM_ADMIN'
ON CONFLICT ON CONSTRAINT uk_role_routes_role_route DO NOTHING;

INSERT INTO menus_cat (id, label, path, icon, description, status, created_at, updated_at) VALUES
  (gen_random_uuid(), 'Parámetros', '/system', 'settings', NULL, 'ACTIVE', NOW(), NOW())
ON CONFLICT ON CONSTRAINT uk_menus_cat_path DO NOTHING;

INSERT INTO role_menus (id, role_id, menu_id, display_order, created_at)
SELECT gen_random_uuid(), ro.id, m.id, 1, NOW()
FROM roles_cat ro
JOIN menus_cat m ON m.path = '/system'
WHERE ro.name = 'SYSTEM_ADMIN'
ON CONFLICT ON CONSTRAINT uk_role_menus_role_menu DO NOTHING;

COMMIT;
