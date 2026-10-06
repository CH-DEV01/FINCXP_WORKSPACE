-- =============================================================================
-- Datos de demostración para DESARROLLO (nunca en QA ni producción).
-- =============================================================================
-- Prerrequisito: base creada por Flyway con todas las migraciones aplicadas,
-- es decir, haber arrancado la API una vez contra la base vacía.
-- Uso (desde CXP_DAV_SERVER_REFACTOR, para que encuentre la plantilla y el manual de db/prod):
--   psql -h localhost -U postgres -d postgres -f db/dev/seed_dev.sql
--
-- Login (SSO stub): POST /api/v1/auth/sso-login  body { "dui": "<DUI>" }
-- DUIs de prueba:
--   00000000-0  ADMIN (operador bancario)
--   11111111-1  PAYER
--   22222222-2  SUPPLIER
--   44444444-4  SYSTEM_ADMIN (parámetros)
-- =============================================================================

\set plantilla_b64 `base64 < db/prod/plantilla_carga_documentos.xlsx 2>/dev/null | tr -d '\n'`
\set manual_b64 `base64 < db/prod/manual_carga_documentos.pdf 2>/dev/null | tr -d '\n'`

BEGIN;

-- -----------------------------------------------------------------------------
-- 3) ENTIDADES DEMO
-- -----------------------------------------------------------------------------
INSERT INTO entities (id, code, nit, name, status, entity_type_id, created_at, updated_at) VALUES
  (
    'a2000000-0000-4000-8000-000000000001',
    'DAVIVI',
    '06140000000010',
    'Davivienda Factoraje (Interno)',
    'ACTIVE',
    'a1000000-0000-4000-8000-000000000003',
    NOW(), NOW()
  ),
  (
    'a2000000-0000-4000-8000-000000000002',
    'PAGDEMO',
    '06140000000021',
    'Empresa Pagadora Demo S.A. de C.V.',
    'ACTIVE',
    'a1000000-0000-4000-8000-000000000001',
    NOW(), NOW()
  ),
  (
    'a2000000-0000-4000-8000-000000000003',
    'PROVDEM',
    '06140000000032',
    'Proveedor Demo El Salvador S.A. de C.V.',
    'ACTIVE',
    'a1000000-0000-4000-8000-000000000002',
    NOW(), NOW()
  )
ON CONFLICT (nit) DO NOTHING;

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
    '000000000',
    'ACTIVE',
    'a2000000-0000-4000-8000-000000000001',
    'b1000000-0000-4000-8000-000000000001',
    NOW(), NOW()
  ),
  (
    'a4000000-0000-4000-8000-000000000002',
    'Maria', 'Pagadora',
    'payer.demo@empresa.local',
    '111111111',
    'ACTIVE',
    'a2000000-0000-4000-8000-000000000002',
    'b1000000-0000-4000-8000-000000000003',
    NOW(), NOW()
  ),
  (
    'a4000000-0000-4000-8000-000000000003',
    'Carlos', 'Proveedor',
    'supplier.demo@empresa.local',
    '222222222',
    'ACTIVE',
    'a2000000-0000-4000-8000-000000000003',
    'b1000000-0000-4000-8000-000000000005',
    NOW(), NOW()
  ),
  (
    'a4000000-0000-4000-8000-000000000005',
    'Sofia', 'Sistemas',
    'sysadmin.factoraje@davivienda.local',
    '444444444',
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

-- -----------------------------------------------------------------------------
-- PLANTILLA Y MANUAL DE CARGA (los mismos que el ADMIN publica en producción). Si un
-- archivo no existe se omite. Se generan con:
--   python3 db/prod/generar_plantilla_carga.py
--   java -cp ~/.m2/repository/com/github/librepdf/openpdf/1.3.39/openpdf-1.3.39.jar db/prod/GenerarManualCarga.java
-- -----------------------------------------------------------------------------
INSERT INTO upload_resources (id, resource_type, file_name, file_content, file_size, created_at, updated_at)
SELECT 'a8000000-0000-4000-8000-000000000001', 'TEMPLATE', 'plantilla_carga_documentos.xlsx',
       decode(:'plantilla_b64', 'base64'), length(decode(:'plantilla_b64', 'base64')), NOW(), NOW()
WHERE length(:'plantilla_b64') > 0
ON CONFLICT (resource_type) DO NOTHING;

INSERT INTO upload_resources (id, resource_type, file_name, file_content, file_size, created_at, updated_at)
SELECT 'a8000000-0000-4000-8000-000000000002', 'MANUAL', 'manual_carga_documentos.pdf',
       decode(:'manual_b64', 'base64'), length(decode(:'manual_b64', 'base64')), NOW(), NOW()
WHERE length(:'manual_b64') > 0
ON CONFLICT (resource_type) DO NOTHING;

COMMIT;
