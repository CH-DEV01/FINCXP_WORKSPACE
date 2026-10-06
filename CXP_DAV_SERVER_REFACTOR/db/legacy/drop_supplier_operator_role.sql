-- Elimina el rol SUPPLIER-OPERATOR. Sus usuarios pasan a SUPPLIER, que es el rol
-- que ahora asigna la carga masiva a los operarios de proveedor.
-- Uso:
--   psql -h localhost -U postgres -d postgres -f db/drop_supplier_operator_role.sql

BEGIN;

UPDATE users
SET role_id = (SELECT id FROM roles_cat WHERE name = 'SUPPLIER'),
    updated_at = NOW()
WHERE role_id = (SELECT id FROM roles_cat WHERE name = 'SUPPLIER-OPERATOR');

DELETE FROM role_routes WHERE role_id = (SELECT id FROM roles_cat WHERE name = 'SUPPLIER-OPERATOR');
DELETE FROM role_menus  WHERE role_id = (SELECT id FROM roles_cat WHERE name = 'SUPPLIER-OPERATOR');
DELETE FROM roles_cat   WHERE name = 'SUPPLIER-OPERATOR';

COMMIT;
