-- Crea el tipo de entidad BANCO (COD_003) y migra a él la entidad interna del banco, que
-- estaba registrada como PAGADOR. La aplicación exige que los usuarios ADMIN, SYSTEM_ADMIN
-- y OPERATOR pertenezcan a una entidad BANCO, PAYER a una PAGADOR y SUPPLIER a una PROVEEDOR.
--
-- La entidad interna se identifica por su código (variable bank_code, por defecto DAVIVI;
-- cámbiela si en el ambiente es otro). Aborta si no existe o si tiene líneas de crédito o
-- convenios como pagador. Al final avisa (sin abortar) de los usuarios cuyo rol no
-- corresponde al tipo de su entidad; esos usuarios no podrán editarse hasta corregirlos.
-- Idempotente. SQL puro: funciona en psql, DBeaver o cualquier cliente.

BEGIN;

INSERT INTO entity_types_cat (id, code, name, status, created_at, updated_at) VALUES
  ('a1000000-0000-4000-8000-000000000003', 'COD_003', 'BANCO', 'ACTIVE', NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

DO $$
DECLARE
    bank_code  TEXT := 'DAVIVI';
    bank_id    UUID;
    mismatches TEXT;
BEGIN
    SELECT id INTO bank_id FROM entities WHERE code = bank_code;
    IF bank_id IS NULL THEN
        RAISE EXCEPTION 'No existe la entidad con código %. Ajuste bank_code en el script.', bank_code;
    END IF;

    IF EXISTS (SELECT 1 FROM credit_facilities WHERE payer_id = bank_id)
       OR EXISTS (SELECT 1 FROM master_agreements WHERE payer_id = bank_id) THEN
        RAISE EXCEPTION 'La entidad % tiene líneas de crédito o convenios como pagador; no se migra.', bank_code;
    END IF;

    UPDATE entities
    SET entity_type_id = (SELECT id FROM entity_types_cat WHERE code = 'COD_003'),
        updated_at = NOW()
    WHERE id = bank_id;

    SELECT string_agg(u.dui || ' (' || r.name || ') -> ' || e.name || ' [' || t.name || ']', E'\n')
    INTO mismatches
    FROM users u
    JOIN roles_cat r ON r.id = u.role_id
    JOIN entities e ON e.id = u.entity_id
    JOIN entity_types_cat t ON t.id = e.entity_type_id
    WHERE (r.name IN ('ADMIN', 'SYSTEM_ADMIN', 'OPERATOR') AND t.code <> 'COD_003')
       OR (r.name = 'PAYER' AND t.code <> 'COD_001')
       OR (r.name = 'SUPPLIER' AND t.code <> 'COD_002');

    IF mismatches IS NOT NULL THEN
        RAISE WARNING E'Usuarios con rol incompatible con el tipo de su entidad:\n%', mismatches;
    END IF;
END $$;

COMMIT;
