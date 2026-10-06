-- Elimina el rol AUTHORIZING_TWO_MODE_AUTH. Tenía las mismas pantallas que PAYER;
-- sus usuarios pasan a PAYER.
-- Uso:
--   psql -h localhost -U postgres -d postgres -f db/drop_authorizing_role.sql

BEGIN;

UPDATE users
SET role_id = (SELECT id FROM roles_cat WHERE name = 'PAYER'),
    updated_at = NOW()
WHERE role_id = (SELECT id FROM roles_cat WHERE name = 'AUTHORIZING_TWO_MODE_AUTH');

DELETE FROM role_routes WHERE role_id = (SELECT id FROM roles_cat WHERE name = 'AUTHORIZING_TWO_MODE_AUTH');
DELETE FROM role_menus  WHERE role_id = (SELECT id FROM roles_cat WHERE name = 'AUTHORIZING_TWO_MODE_AUTH');
DELETE FROM roles_cat   WHERE name = 'AUTHORIZING_TWO_MODE_AUTH';

COMMIT;
