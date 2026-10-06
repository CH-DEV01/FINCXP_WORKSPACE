-- =============================================================================
-- Rutas y menús por rol (routes_cat, menus_cat, role_routes, role_menus y
-- roles_cat.default_route). Carga la configuración que antes estaba fija en
-- AuthService.
-- =============================================================================
-- Prerrequisito: arrancar la API una vez para que Hibernate cree las tablas.
-- Idempotente: puede ejecutarse varias veces.
-- Uso:
--   psql -h localhost -U postgres -d postgres -f db/seed_routes_menus.sql
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 1) Ruta base del layout de cada rol
-- -----------------------------------------------------------------------------
UPDATE roles_cat SET default_route = '/admin',    updated_at = NOW() WHERE name = 'ADMIN';
UPDATE roles_cat SET default_route = '/supplier', updated_at = NOW() WHERE name = 'SUPPLIER';
UPDATE roles_cat SET default_route = '/payer',    updated_at = NOW() WHERE name = 'PAYER';
UPDATE roles_cat SET default_route = '/system',   updated_at = NOW() WHERE name = 'SYSTEM_ADMIN';

-- -----------------------------------------------------------------------------
-- 2) Catálogo de rutas (path relativo a la ruta base + componente del cliente)
-- -----------------------------------------------------------------------------
INSERT INTO routes_cat (id, path, component_name, description, status, created_at, updated_at) VALUES
  (gen_random_uuid(), '',                             'Menu',                   'Menú del administrador',          'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'agreement-management',         'AgreementManagement',    'Gestión de acuerdos',             'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'master-agreement-management',  'AgreementManagement',    'Gestión de convenios comerciales','ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'supplier-management',          'SupplierManagement',     'Gestión de proveedores',          'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'payer-management-admin',       'PayerManagementAdmin',   'Gestión de pagadores',            'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'user-management',              'UserManagement',         'Gestión de usuarios',             'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'upload-file-admin',            'UploadFilePageAdmin',    'Carga de documentos (admin)',     'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'term-management',              'TermManagement',         'Gestión de plazos',               'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'holiday-management',           'HolidayManagement',      'Gestión de feriados',             'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'payer-credit-line-management', 'PayerCreditLineManager', 'Gestión de cupos de crédito',     'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'disbursement-terminal',        'ControlTerminal',        'Terminal de desembolsos',         'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'batches-history',              'BatchLog',               'Bitácora de lotes',               'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'param-management',             'ParamManagement',        'Gestión de parámetros',           'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), '',                             'ParamManagement',        'Gestión de parámetros (inicio)',  'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), '',                             'SelectDocuments',        'Selección de documentos',         'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'documents-history',            'DocumentLog',            'Bitácora del proveedor',          'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), '',                             'UploadFilePage',         'Carga de documentos (pagador)',   'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'documents-history',            'PayerDocumentLog',       'Bitácora del pagador',            'ACTIVE', NOW(), NOW())
ON CONFLICT ON CONSTRAINT uk_routes_cat_path_component DO NOTHING;

-- -----------------------------------------------------------------------------
-- 3) Rutas por rol (param-management queda en el catálogo sin asignar; los
--    parámetros los administra SYSTEM_ADMIN desde su ruta base)
-- -----------------------------------------------------------------------------
INSERT INTO role_routes (id, role_id, route_id, is_index, created_at)
SELECT gen_random_uuid(), ro.id, rt.id, a.is_index, NOW()
FROM (VALUES
  ('ADMIN',   '',                             'Menu',                   TRUE),
  ('ADMIN',   'agreement-management',         'AgreementManagement',    FALSE),
  ('ADMIN',   'master-agreement-management',  'AgreementManagement',    FALSE),
  ('ADMIN',   'supplier-management',          'SupplierManagement',     FALSE),
  ('ADMIN',   'payer-management-admin',       'PayerManagementAdmin',   FALSE),
  ('ADMIN',   'user-management',              'UserManagement',         FALSE),
  ('ADMIN',   'upload-file-admin',            'UploadFilePageAdmin',    FALSE),
  ('ADMIN',   'term-management',              'TermManagement',         FALSE),
  ('ADMIN',   'holiday-management',           'HolidayManagement',      FALSE),
  ('ADMIN',   'payer-credit-line-management', 'PayerCreditLineManager', FALSE),
  ('ADMIN',   'disbursement-terminal',        'ControlTerminal',        FALSE),
  ('ADMIN',   'batches-history',              'BatchLog',               FALSE),
  ('SUPPLIER',                  '',                  'SelectDocuments',  TRUE),
  ('SUPPLIER',                  'documents-history', 'DocumentLog',      FALSE),
  ('PAYER',                     '',                  'UploadFilePage',   TRUE),
  ('PAYER',                     'documents-history', 'PayerDocumentLog', FALSE),
  ('SYSTEM_ADMIN',              '',                  'ParamManagement',  TRUE)
) AS a(role_name, path, component_name, is_index)
JOIN roles_cat ro ON ro.name = a.role_name
JOIN routes_cat rt ON rt.path = a.path AND rt.component_name = a.component_name
ON CONFLICT ON CONSTRAINT uk_role_routes_role_route DO NOTHING;

-- -----------------------------------------------------------------------------
-- 4) Catálogo de menús (path absoluto)
-- -----------------------------------------------------------------------------
INSERT INTO menus_cat (id, label, path, icon, description, status, created_at, updated_at) VALUES
  (gen_random_uuid(), 'Inicio',               '/admin',                              'home',        NULL, 'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'Acuerdos',             '/admin/agreement-management',         'file-text',   NULL, 'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'Proveedores',          '/admin/supplier-management',          'truck',       NULL, 'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'Pagadores',            '/admin/payer-management-admin',       'credit-card', NULL, 'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'Usuarios',             '/admin/user-management',              'users',       NULL, 'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'Feriados',             '/admin/holiday-management',           'calendar',    NULL, 'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'Desembolsos',          '/admin/disbursement-terminal',        'monitor',     NULL, 'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'Bitácora de lotes',    '/admin/batches-history',              'clock',       NULL, 'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'Seleccionar Acuerdos', '/select-agreement',                   'list',        NULL, 'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'Documentos',           '/supplier',                           'file',        NULL, 'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'Historial',            '/supplier/documents-history',         'clock',       NULL, 'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'Cargar Archivos',      '/payer',                              'upload',      NULL, 'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'Bitácora',             '/payer/documents-history',            'clock',       NULL, 'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'Parámetros',           '/system',                             'settings',    NULL, 'ACTIVE', NOW(), NOW())
ON CONFLICT ON CONSTRAINT uk_menus_cat_path DO NOTHING;

-- -----------------------------------------------------------------------------
-- 5) Menús por rol
-- -----------------------------------------------------------------------------
INSERT INTO role_menus (id, role_id, menu_id, display_order, created_at)
SELECT gen_random_uuid(), ro.id, m.id, a.display_order, NOW()
FROM (VALUES
  ('ADMIN',   '/admin',                         1),
  ('ADMIN',   '/admin/agreement-management',    2),
  ('ADMIN',   '/admin/supplier-management',     3),
  ('ADMIN',   '/admin/payer-management-admin',  4),
  ('ADMIN',   '/admin/user-management',         5),
  ('ADMIN',   '/admin/holiday-management',      6),
  ('ADMIN',   '/admin/disbursement-terminal',   7),
  ('ADMIN',   '/admin/batches-history',         8),
  ('SUPPLIER',                  '/select-agreement',           1),
  ('SUPPLIER',                  '/supplier',                   2),
  ('SUPPLIER',                  '/supplier/documents-history', 3),
  ('PAYER',                     '/payer',                      1),
  ('PAYER',                     '/payer/documents-history',    2),
  ('SYSTEM_ADMIN',              '/system',                     1)
) AS a(role_name, path, display_order)
JOIN roles_cat ro ON ro.name = a.role_name
JOIN menus_cat m ON m.path = a.path
ON CONFLICT ON CONSTRAINT uk_role_menus_role_menu DO NOTHING;

COMMIT;
