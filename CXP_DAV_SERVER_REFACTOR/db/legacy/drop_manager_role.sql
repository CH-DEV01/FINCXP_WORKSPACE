-- Elimina el rol MANAGER. Tenía los mismos permisos que ADMIN, que queda como
-- único rol administrativo; sus usuarios pasan a ADMIN.
-- Uso:
--   psql -h localhost -U postgres -d postgres -f db/drop_manager_role.sql

BEGIN;

UPDATE users
SET role_id = (SELECT id FROM roles_cat WHERE name = 'ADMIN'),
    updated_at = NOW()
WHERE role_id = (SELECT id FROM roles_cat WHERE name = 'MANAGER');

DELETE FROM role_routes WHERE role_id = (SELECT id FROM roles_cat WHERE name = 'MANAGER');
DELETE FROM role_menus  WHERE role_id = (SELECT id FROM roles_cat WHERE name = 'MANAGER');
DELETE FROM roles_cat   WHERE name = 'MANAGER';

COMMIT;
