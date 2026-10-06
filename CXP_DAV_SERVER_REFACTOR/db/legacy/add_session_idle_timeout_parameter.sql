-- Minutos de inactividad tras los que el cliente cierra la sesión (1 a 480).
-- Idempotente: si la llave ya existe se respeta el valor configurado en el ambiente.
BEGIN;

INSERT INTO system_parameters (id, param_key, param_value, created_at, updated_at) VALUES
  (gen_random_uuid(), 'SESSION_IDLE_TIMEOUT_MINUTES', '15', NOW(), NOW())
ON CONFLICT (param_key) DO NOTHING;

COMMIT;
