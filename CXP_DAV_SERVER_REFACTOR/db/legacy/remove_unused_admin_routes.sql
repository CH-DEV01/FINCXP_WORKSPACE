-- =============================================================================
-- Elimina las rutas del administrador que el cliente ya no implementa:
-- Bitácora de documentos (DocumentManagement) y Detalle de documento (ViewDocument).
-- =============================================================================
-- Idempotente: puede ejecutarse varias veces.
-- Uso:
--   psql -h localhost -U postgres -d postgres -f db/remove_unused_admin_routes.sql
-- =============================================================================

BEGIN;

DELETE FROM role_routes
WHERE route_id IN (
    SELECT id FROM routes_cat WHERE component_name IN ('DocumentManagement', 'ViewDocument')
);

DELETE FROM routes_cat
WHERE component_name IN ('DocumentManagement', 'ViewDocument');

COMMIT;
