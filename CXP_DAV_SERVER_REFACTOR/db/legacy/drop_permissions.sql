-- Elimina el catálogo de permisos y su relación con roles.
-- Uso:
--   psql -h localhost -U postgres -d postgres -f db/drop_permissions.sql

BEGIN;

DROP TABLE IF EXISTS role_permissions;
DROP TABLE IF EXISTS permissions_cat;

COMMIT;
