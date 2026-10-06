-- Parámetros que antes estaban fijos en el código. Idempotente: si la llave ya
-- existe se respeta el valor configurado en el ambiente.
-- CORS_ALLOWED_ORIGINS debe ajustarse en cada ambiente con la URL real del cliente.
BEGIN;

INSERT INTO system_parameters (id, param_key, param_value, created_at, updated_at) VALUES
  (gen_random_uuid(), 'DEFAULT_CREDIT_THRESHOLD', '0.80', NOW(), NOW()),
  (gen_random_uuid(), 'DISBURSEMENT_CUTOFF_TIME', '15:00', NOW(), NOW()),
  (gen_random_uuid(), 'DUE_DATE_GRACE_DAYS', '5', NOW(), NOW()),
  (gen_random_uuid(), 'MAX_INVOICE_AGE_DAYS', '120', NOW(), NOW()),
  (gen_random_uuid(), 'UPLOAD_ALLOWED_EXTENSIONS', '.xlsx,.xls', NOW(), NOW()),
  (gen_random_uuid(), 'UPLOAD_MAX_FILE_SIZE_MB', '5', NOW(), NOW()),
  (gen_random_uuid(), 'UPLOAD_MAX_ROWS', '5000', NOW(), NOW()),
  (gen_random_uuid(), 'CORS_ALLOWED_ORIGINS', 'http://localhost:5173,https://devpay.davivienda.com.sv', NOW(), NOW()),
  (gen_random_uuid(), 'JWT_EXPIRATION_MINUTES', '1440', NOW(), NOW())
ON CONFLICT (param_key) DO NOTHING;

COMMIT;
