-- =============================================================================
-- Renombra el componente del cliente 'UserManagement2' a 'UserManagement' en
-- routes_cat (el cliente ya no registra 'UserManagement2').
-- =============================================================================
-- Idempotente: puede ejecutarse varias veces. Si seed_routes_menus.sql ya se
-- ejecutó con el nombre nuevo y existen ambas filas, se reasignan los roles a la
-- fila nueva y se elimina la vieja.
-- Uso:
--   psql -h localhost -U postgres -d postgres -f db/rename_user_management_component.sql
-- =============================================================================

BEGIN;

-- Roles que ya tienen ambas rutas: se descarta la asignación vieja
DELETE FROM role_routes rr
USING routes_cat old_rt, routes_cat new_rt, role_routes rr_new
WHERE rr.route_id = old_rt.id
  AND old_rt.component_name = 'UserManagement2'
  AND new_rt.path = old_rt.path
  AND new_rt.component_name = 'UserManagement'
  AND rr_new.route_id = new_rt.id
  AND rr_new.role_id = rr.role_id;

UPDATE role_routes rr
SET route_id = new_rt.id
FROM routes_cat old_rt, routes_cat new_rt
WHERE rr.route_id = old_rt.id
  AND old_rt.component_name = 'UserManagement2'
  AND new_rt.path = old_rt.path
  AND new_rt.component_name = 'UserManagement';

DELETE FROM routes_cat old_rt
USING routes_cat new_rt
WHERE old_rt.component_name = 'UserManagement2'
  AND new_rt.path = old_rt.path
  AND new_rt.component_name = 'UserManagement';

UPDATE routes_cat
SET component_name = 'UserManagement', updated_at = NOW()
WHERE component_name = 'UserManagement2';

COMMIT;
