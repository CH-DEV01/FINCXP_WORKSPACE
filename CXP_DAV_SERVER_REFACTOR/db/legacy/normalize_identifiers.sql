-- Deja los identificadores existentes en el formato con el que ahora se guardan y buscan:
--   entities.niu, entities.nit, users.dui, bank_accounts.account_number -> sin guiones ni espacios
--   users.email -> minúsculas
--   documents.generation_code, control_number, received_stamp -> mayúsculas (conservan guiones)
--
-- Antes de modificar revisa que la normalización no cree duplicados (todas estas columnas,
-- salvo las del DTE, tienen restricción UNIQUE). Si encuentra alguno, aborta y los lista.
--
-- Después de ejecutarlo los usuarios con DUI que tenía guion deben volver a iniciar sesión:
-- su JWT trae el DUI anterior en el subject.

BEGIN;

DO $$
DECLARE
    conflicts TEXT;
BEGIN
    SELECT string_agg(detail, E'\n') INTO conflicts FROM (
        SELECT 'entities.niu ' || regexp_replace(niu, '[\s-]', '', 'g') || ': ' || string_agg(niu, ', ') AS detail
        FROM entities GROUP BY regexp_replace(niu, '[\s-]', '', 'g') HAVING count(*) > 1
        UNION ALL
        SELECT 'entities.nit ' || regexp_replace(nit, '[\s-]', '', 'g') || ': ' || string_agg(nit, ', ')
        FROM entities GROUP BY regexp_replace(nit, '[\s-]', '', 'g') HAVING count(*) > 1
        UNION ALL
        SELECT 'users.dui ' || regexp_replace(dui, '[\s-]', '', 'g') || ': ' || string_agg(dui, ', ')
        FROM users GROUP BY regexp_replace(dui, '[\s-]', '', 'g') HAVING count(*) > 1
        UNION ALL
        SELECT 'users.email ' || lower(trim(email)) || ': ' || string_agg(email, ', ')
        FROM users GROUP BY lower(trim(email)) HAVING count(*) > 1
        UNION ALL
        SELECT 'bank_accounts.account_number ' || regexp_replace(account_number, '[\s-]', '', 'g') || ': ' || string_agg(account_number, ', ')
        FROM bank_accounts GROUP BY regexp_replace(account_number, '[\s-]', '', 'g') HAVING count(*) > 1
    ) c;

    IF conflicts IS NOT NULL THEN
        RAISE EXCEPTION E'La normalización generaría duplicados. Resuélvalos antes de continuar:\n%', conflicts;
    END IF;
END $$;

UPDATE entities SET niu = regexp_replace(niu, '[\s-]', '', 'g') WHERE niu ~ '[\s-]';
UPDATE entities SET nit = regexp_replace(nit, '[\s-]', '', 'g') WHERE nit ~ '[\s-]';
UPDATE users SET dui = regexp_replace(dui, '[\s-]', '', 'g') WHERE dui ~ '[\s-]';
UPDATE users SET email = lower(trim(email)) WHERE email <> lower(trim(email));
UPDATE bank_accounts SET account_number = regexp_replace(account_number, '[\s-]', '', 'g') WHERE account_number ~ '[\s-]';

-- En facturas PAPER estos campos quedaron como '' en lugar de NULL.
UPDATE documents SET generation_code = NULLIF(upper(trim(generation_code)), '')
WHERE generation_code IS NOT NULL AND generation_code IS DISTINCT FROM NULLIF(upper(trim(generation_code)), '');
UPDATE documents SET control_number = NULLIF(upper(trim(control_number)), '')
WHERE control_number IS NOT NULL AND control_number IS DISTINCT FROM NULLIF(upper(trim(control_number)), '');
UPDATE documents SET received_stamp = NULLIF(upper(trim(received_stamp)), '')
WHERE received_stamp IS NOT NULL AND received_stamp IS DISTINCT FROM NULLIF(upper(trim(received_stamp)), '');

-- Solo informativo: registros que siguen sin cumplir el formato y DTE repetidos (no se modifican).
SELECT 'entities.nit' AS campo, nit AS valor FROM entities WHERE nit !~ '^\d{14}$'
UNION ALL SELECT 'entities.niu', niu FROM entities WHERE niu !~ '^\d{1,25}$'
UNION ALL SELECT 'users.dui', dui FROM users WHERE dui !~ '^\d{9}$'
UNION ALL SELECT 'bank_accounts.account_number', account_number FROM bank_accounts WHERE account_number !~ '^\d+$'
UNION ALL SELECT 'documents.generation_code repetido', generation_code FROM documents
    WHERE generation_code IS NOT NULL GROUP BY generation_code HAVING count(*) > 1
UNION ALL SELECT 'documents.control_number repetido', control_number FROM documents
    WHERE control_number IS NOT NULL GROUP BY control_number HAVING count(*) > 1
UNION ALL SELECT 'documents.received_stamp repetido', received_stamp FROM documents
    WHERE received_stamp IS NOT NULL GROUP BY received_stamp HAVING count(*) > 1;

COMMIT;
