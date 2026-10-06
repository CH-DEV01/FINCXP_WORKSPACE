-- =============================================================================
-- Seed PostgreSQL — CXP_DAV_SERVER_REFACTOR (Factoraje / Financiamiento CXP)
-- =============================================================================
-- Prerrequisito: arrancar la API una vez con ddl-auto=update para crear tablas.
-- Uso:
--   psql -h localhost -U postgres -d postgres -f db/seed_dev.sql
--
-- Login (SSO stub): POST /api/v1/auth/sso-login  body { "dui": "<DUI>" }
--   Solo valida que el DUI exista; no usa password.
--
-- DUIs de prueba:
--   00000000-0  ADMIN
--   11111111-1  PAYER
--   22222222-2  SUPPLIER
--   33333333-3  OPERATOR (banco)
--   44444444-4  SYSTEM_ADMIN (parámetros)
--
-- Nota: UUID solo admite hex (0-9, a-f).
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 1) CATÁLOGOS BASE
-- -----------------------------------------------------------------------------

INSERT INTO entity_types_cat (id, code, name, status, created_at, updated_at) VALUES
  ('a1000000-0000-4000-8000-000000000001', 'COD_001', 'PAGADOR',   'ACTIVE', NOW(), NOW()),
  ('a1000000-0000-4000-8000-000000000002', 'COD_002', 'PROVEEDOR', 'ACTIVE', NOW(), NOW()),
  ('a1000000-0000-4000-8000-000000000003', 'COD_003', 'BANCO',     'ACTIVE', NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

INSERT INTO roles_cat (id, name, description, status, created_at, updated_at) VALUES
  ('b1000000-0000-4000-8000-000000000001', 'ADMIN',                    'Administrador del sistema',                'ACTIVE', NOW(), NOW()),
  ('b1000000-0000-4000-8000-000000000003', 'PAYER',                    'Usuario pagador',                           'ACTIVE', NOW(), NOW()),
  ('b1000000-0000-4000-8000-000000000005', 'SUPPLIER',                 'Usuario proveedor',                         'ACTIVE', NOW(), NOW()),
  ('b1000000-0000-4000-8000-000000000006', 'OPERATOR',                 'Operador bancario (desembolsos)',            'ACTIVE', NOW(), NOW()),
  ('b1000000-0000-4000-8000-000000000007', 'SYSTEM_ADMIN',             'Administrador de parámetros del sistema',    'ACTIVE', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

INSERT INTO payment_policies_cat (id, code, days_count, description, status, created_at, updated_at) VALUES
  ('e1000000-0000-4000-8000-000000000030', 'P30', 30, 'Pago a 30 días', 'ACTIVE', NOW(), NOW()),
  ('e1000000-0000-4000-8000-000000000045', 'P45', 45, 'Pago a 45 días', 'ACTIVE', NOW(), NOW()),
  ('e1000000-0000-4000-8000-000000000060', 'P60', 60, 'Pago a 60 días', 'ACTIVE', NOW(), NOW()),
  ('e1000000-0000-4000-8000-000000000090', 'P90', 90, 'Pago a 90 días', 'ACTIVE', NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

-- type: T_PLUS_N  -> desembolso offset_days días hábiles después de la solicitud.
--       WEEKDAYS  -> primer día de weekdays que caiga al menos offset_days hábiles después.
INSERT INTO disbursement_policies_cat (id, code, name, description, type, weekdays, offset_days, status, created_at, updated_at) VALUES
  ('f1000000-0000-4000-8000-000000000001', 'T_PLUS_1',     'T+1',          'Desembolso al siguiente día hábil', 'T_PLUS_N', NULL,     1, 'ACTIVE', NOW(), NOW()),
  ('f1000000-0000-4000-8000-000000000002', 'ONLY_FRIDAYS', 'Solo viernes', 'Desembolso únicamente los viernes', 'WEEKDAYS', 'FRIDAY', 1, 'ACTIVE', NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

INSERT INTO term_types_cat (id, term_name, unique_code, status, created_at, updated_at) VALUES
  ('aa000000-0000-4000-8000-000000000001', 'Términos y condiciones — Pagador',   'PAYER_TERM_TYPE',    'ACTIVE', NOW(), NOW()),
  ('aa000000-0000-4000-8000-000000000002', 'Términos y condiciones — Proveedor', 'SUPPLIER_TERM_TYPE', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Sin contenido: lo carga db/terms_markdown_content.sql, que debe ejecutarse después de este seed.
INSERT INTO term_versions_cat (
  id, version_number, publication_date, status, term_type_id, created_at, updated_at
) VALUES
  (
    'ab000000-0000-4000-8000-000000000001',
    '1.0',
    CURRENT_DATE,
    'ACTIVE',
    'aa000000-0000-4000-8000-000000000001',
    NOW(), NOW()
  ),
  (
    'ab000000-0000-4000-8000-000000000002',
    '1.0',
    CURRENT_DATE,
    'ACTIVE',
    'aa000000-0000-4000-8000-000000000002',
    NOW(), NOW()
  )
ON CONFLICT (id) DO NOTHING;

INSERT INTO action_documents_cat (id, name, description, created_at, updated_at) VALUES
  ('ac000000-0000-4000-8000-000000000001', 'UPLOAD',   'Carga de lote de facturas',   NOW(), NOW()),
  ('ac000000-0000-4000-8000-000000000002', 'APPROVE',  'Aprobación de documentos',    NOW(), NOW()),
  ('ac000000-0000-4000-8000-000000000003', 'REQUEST',  'Solicitud de financiamiento', NOW(), NOW()),
  ('ac000000-0000-4000-8000-000000000004', 'DISBURSE', 'Confirmación de desembolso',  NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO bank_holidays_cat (id, holiday_date, description, status, created_at, updated_at) VALUES
  ('ad000000-0000-4000-8000-000000000001', '2026-01-01', 'Año Nuevo',              'ACTIVE', NOW(), NOW()),
  ('ad000000-0000-4000-8000-000000000002', '2026-05-01', 'Día del Trabajo',         'ACTIVE', NOW(), NOW()),
  ('ad000000-0000-4000-8000-000000000003', '2026-08-06', 'Fiestas Agostinas',       'ACTIVE', NOW(), NOW()),
  ('ad000000-0000-4000-8000-000000000004', '2026-09-15', 'Día de la Independencia', 'ACTIVE', NOW(), NOW()),
  ('ad000000-0000-4000-8000-000000000005', '2026-11-02', 'Día de los Difuntos',     'ACTIVE', NOW(), NOW()),
  ('ad000000-0000-4000-8000-000000000006', '2026-12-25', 'Navidad',                 'ACTIVE', NOW(), NOW())
ON CONFLICT (holiday_date) DO NOTHING;

INSERT INTO system_parameters (id, param_key, param_value, created_at, updated_at) VALUES
  ('ae000000-0000-4000-8000-000000000001', 'IVA_RATE', '0.13', NOW(), NOW()),
  ('ae000000-0000-4000-8000-000000000002', 'DEFAULT_CREDIT_THRESHOLD', '0.80', NOW(), NOW()),
  ('ae000000-0000-4000-8000-000000000003', 'DISBURSEMENT_CUTOFF_TIME', '15:00', NOW(), NOW()),
  ('ae000000-0000-4000-8000-000000000004', 'DUE_DATE_GRACE_DAYS', '5', NOW(), NOW()),
  ('ae000000-0000-4000-8000-000000000005', 'MAX_INVOICE_AGE_DAYS', '120', NOW(), NOW()),
  ('ae000000-0000-4000-8000-000000000006', 'UPLOAD_ALLOWED_EXTENSIONS', '.xlsx,.xls', NOW(), NOW()),
  ('ae000000-0000-4000-8000-000000000007', 'UPLOAD_MAX_FILE_SIZE_MB', '5', NOW(), NOW()),
  ('ae000000-0000-4000-8000-000000000008', 'UPLOAD_MAX_ROWS', '5000', NOW(), NOW()),
  ('ae000000-0000-4000-8000-000000000009', 'CORS_ALLOWED_ORIGINS', 'http://localhost:5173,https://devpay.davivienda.com.sv', NOW(), NOW()),
  ('ae000000-0000-4000-8000-000000000010', 'JWT_EXPIRATION_MINUTES', '1440', NOW(), NOW()),
  ('ae000000-0000-4000-8000-000000000011', 'SESSION_IDLE_TIMEOUT_MINUTES', '15', NOW(), NOW())
ON CONFLICT (param_key) DO UPDATE SET param_value = EXCLUDED.param_value, updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 2) COLUMNAS DEL TEMPLATE EXCEL
-- -----------------------------------------------------------------------------
INSERT INTO excel_template_columns (
  id, excel_column_name, logical_dto_field, is_required, is_active, created_at, updated_at
) VALUES
  ('af000000-0000-4000-8000-000000000001', 'Fecha Emision',            'issueDate',              true,  true, NOW(), NOW()),
  ('af000000-0000-4000-8000-000000000002', 'Monto',                    'nominalAmount',          true,  true, NOW(), NOW()),
  ('af000000-0000-4000-8000-000000000003', 'Numero Documento',         'documentNumber',         true,  true, NOW(), NOW()),
  ('af000000-0000-4000-8000-000000000004', 'Codigo Generacion',        'generationCode',         false, true, NOW(), NOW()),
  ('af000000-0000-4000-8000-000000000005', 'Sello Recepcion',          'receivedStamp',          false, true, NOW(), NOW()),
  ('af000000-0000-4000-8000-000000000006', 'Numero Control',           'controlNumber',          false, true, NOW(), NOW()),
  ('af000000-0000-4000-8000-000000000007', 'Metodo Emision',           'issuanceMethod',         true,  true, NOW(), NOW()),
  ('af000000-0000-4000-8000-000000000008', 'Tipo Factura',             'invoiceType',            true,  true, NOW(), NOW()),
  ('af000000-0000-4000-8000-000000000009', 'NIU Proveedor',            'supplierNiu',            true,  true, NOW(), NOW()),
  ('af000000-0000-4000-8000-00000000000a', 'NIT Proveedor',            'supplierNit',            true,  true, NOW(), NOW()),
  ('af000000-0000-4000-8000-00000000000b', 'Nombre Proveedor',         'supplierName',           true,  true, NOW(), NOW()),
  ('af000000-0000-4000-8000-00000000000c', 'Politica Pago',            'paymentPolicy',          true,  true, NOW(), NOW()),
  ('af000000-0000-4000-8000-00000000000d', 'Dia Desembolso',           'disbursementDay',        true,  true, NOW(), NOW()),
  ('af000000-0000-4000-8000-00000000000e', 'DUI Operario',             'operatorDui',            true,  true, NOW(), NOW()),
  ('af000000-0000-4000-8000-00000000000f', 'Correo Operario',          'operatorEmail',          true,  true, NOW(), NOW()),
  ('af000000-0000-4000-8000-000000000010', 'Primer Nombre Operario',   'operatorFirstName',      true,  true, NOW(), NOW()),
  ('af000000-0000-4000-8000-000000000011', 'Segundo Nombre Operario',  'operatorMiddleName',     false, true, NOW(), NOW()),
  ('af000000-0000-4000-8000-000000000012', 'Primer Apellido Operario', 'operatorFirstLastName',  true,  true, NOW(), NOW()),
  ('af000000-0000-4000-8000-000000000013', 'Segundo Apellido Operario','operatorSecondLastName', false, true, NOW(), NOW()),
  ('af000000-0000-4000-8000-000000000014', 'Cuenta Bancaria',          'supplierAccountNumber',  true,  true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- -----------------------------------------------------------------------------
-- 3) ENTIDADES DEMO
-- -----------------------------------------------------------------------------
INSERT INTO entities (id, niu, code, nit, name, status, entity_type_id, created_at, updated_at) VALUES
  (
    'a2000000-0000-4000-8000-000000000001',
    '06140000000001',
    'DAVIVI',
    '0614-000000-001-0',
    'Davivienda Factoraje (Interno)',
    'ACTIVE',
    'a1000000-0000-4000-8000-000000000003',
    NOW(), NOW()
  ),
  (
    'a2000000-0000-4000-8000-000000000002',
    '06140000000002',
    'PAGDEMO',
    '0614-000000-002-1',
    'Empresa Pagadora Demo S.A. de C.V.',
    'ACTIVE',
    'a1000000-0000-4000-8000-000000000001',
    NOW(), NOW()
  ),
  (
    'a2000000-0000-4000-8000-000000000003',
    '06140000000003',
    'PROVDEM',
    '0614-000000-003-2',
    'Proveedor Demo El Salvador S.A. de C.V.',
    'ACTIVE',
    'a1000000-0000-4000-8000-000000000002',
    NOW(), NOW()
  )
ON CONFLICT (niu) DO NOTHING;

-- Todas las cuentas son de Banco Davivienda: no se guarda banco ni tipo de cuenta.
INSERT INTO bank_accounts (
  id, account_number, entity_id, is_main, status, created_at, updated_at
) VALUES
  (
    'a3000000-0000-4000-8000-000000000001',
    '000123456789',
    'a2000000-0000-4000-8000-000000000002',
    true,
    'ACTIVE',
    NOW(), NOW()
  ),
  (
    'a3000000-0000-4000-8000-000000000002',
    '000987654321',
    'a2000000-0000-4000-8000-000000000003',
    true,
    'ACTIVE',
    NOW(), NOW()
  )
ON CONFLICT (account_number) DO NOTHING;

-- -----------------------------------------------------------------------------
-- 4) USUARIOS DEMO
-- -----------------------------------------------------------------------------
INSERT INTO users (
  id, first_name, last_name, email, dui, status, entity_id, role_id, created_at, updated_at
) VALUES
  (
    'a4000000-0000-4000-8000-000000000001',
    'Admin', 'Sistema',
    'admin.factoraje@davivienda.local',
    '00000000-0',
    'ACTIVE',
    'a2000000-0000-4000-8000-000000000001',
    'b1000000-0000-4000-8000-000000000001',
    NOW(), NOW()
  ),
  (
    'a4000000-0000-4000-8000-000000000002',
    'Maria', 'Pagadora',
    'payer.demo@empresa.local',
    '11111111-1',
    'ACTIVE',
    'a2000000-0000-4000-8000-000000000002',
    'b1000000-0000-4000-8000-000000000003',
    NOW(), NOW()
  ),
  (
    'a4000000-0000-4000-8000-000000000003',
    'Carlos', 'Proveedor',
    'supplier.demo@empresa.local',
    '22222222-2',
    'ACTIVE',
    'a2000000-0000-4000-8000-000000000003',
    'b1000000-0000-4000-8000-000000000005',
    NOW(), NOW()
  ),
  (
    'a4000000-0000-4000-8000-000000000004',
    'Luis', 'Operador',
    'operator.factoraje@davivienda.local',
    '33333333-3',
    'ACTIVE',
    'a2000000-0000-4000-8000-000000000001',
    'b1000000-0000-4000-8000-000000000006',
    NOW(), NOW()
  ),
  (
    'a4000000-0000-4000-8000-000000000005',
    'Sofia', 'Sistemas',
    'sysadmin.factoraje@davivienda.local',
    '44444444-4',
    'ACTIVE',
    'a2000000-0000-4000-8000-000000000001',
    'b1000000-0000-4000-8000-000000000007',
    NOW(), NOW()
  )
ON CONFLICT (dui) DO NOTHING;

-- -----------------------------------------------------------------------------
-- 5) LÍNEA DE CRÉDITO + PRICING + CONVENIO MARCO
-- -----------------------------------------------------------------------------
INSERT INTO credit_facilities (
  id, credit_facility_number, facility_limit_amount, amount_in_use, status,
  payer_id, warning_threshold_percentage, created_at, updated_at
) VALUES
  (
    'a5000000-0000-4000-8000-000000000001',
    'CF-PAYER-DEMO-001',
    500000.0000,
    0.0000,
    'ACTIVE',
    'a2000000-0000-4000-8000-000000000002',
    0.80,
    NOW(), NOW()
  )
ON CONFLICT (id) DO NOTHING;

INSERT INTO product_pricing_terms (
  id, interest_rate, commission_rate, calculation_base, status,
  credit_facility_id, created_at, updated_at
) VALUES
  (
    'a6000000-0000-4000-8000-000000000001',
    0.155000,
    0.030000,
    'COMERCIAL_360',
    'ACTIVE',
    'a5000000-0000-4000-8000-000000000001',
    NOW(), NOW()
  )
ON CONFLICT (id) DO NOTHING;

INSERT INTO master_agreements (
  id, agreement_type, status, payer_id, supplier_id,
  payment_policy_id, disbursement_policy_id, created_at, updated_at
) VALUES
  (
    'a7000000-0000-4000-8000-000000000001',
    'STANDARD',
    'ACTIVE',
    'a2000000-0000-4000-8000-000000000002',
    'a2000000-0000-4000-8000-000000000003',
    'e1000000-0000-4000-8000-000000000060',
    'f1000000-0000-4000-8000-000000000001',
    NOW(), NOW()
  )
ON CONFLICT (id) DO NOTHING;

COMMIT;
