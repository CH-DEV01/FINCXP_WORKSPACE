-- Términos y condiciones administrables: el texto legal pasa del cliente a term_versions_cat
-- en Markdown, con estado DRAFT para borradores y hash SHA-256 del texto publicado.
--
-- Qué hace:
--   1. Agrega title, content, acceptance_text, document_url, content_hash y published_by_id.
--   2. Carga en la versión vigente del proveedor el texto que mostraba SelectDocuments.jsx.
--   3. Carga en la versión vigente del pagador el texto que realmente vio el pagador
--      (DocumentUploadCenter.jsx mostraba un resumen de los términos del proveedor), la
--      desactiva y publica una versión 1.1 con un TEXTO PROVISIONAL para el pagador.
--      Así las aceptaciones ya registradas siguen apuntando al texto que se aceptó.
--   4. Calcula el hash, aplica NOT NULL, agrega DRAFT al CHECK de status, un índice único
--      de una versión ACTIVE por tipo y otro de número de versión por tipo.
--   5. Elimina legal_text (solo tenía enlaces de ejemplo). Si apuntaba a un https real que
--      no sea example.local, lo conserva en document_url.
--
-- Es idempotente: se puede ejecutar más de una vez. Debe ejecutarse antes de crear versiones
-- desde la pantalla de administración (legal_text NOT NULL bloquea los INSERT).
-- Uso:
--   psql -h localhost -U postgres -d postgres -f db/terms_markdown_content.sql

BEGIN;

ALTER TABLE term_versions_cat ADD COLUMN IF NOT EXISTS title           varchar(255);
ALTER TABLE term_versions_cat ADD COLUMN IF NOT EXISTS content         text;
ALTER TABLE term_versions_cat ADD COLUMN IF NOT EXISTS acceptance_text varchar(500);
ALTER TABLE term_versions_cat ADD COLUMN IF NOT EXISTS document_url    varchar(500);
ALTER TABLE term_versions_cat ADD COLUMN IF NOT EXISTS content_hash    varchar(64);
ALTER TABLE term_versions_cat ADD COLUMN IF NOT EXISTS published_by_id uuid;
ALTER TABLE term_versions_cat ALTER COLUMN publication_date DROP NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
          ON kcu.constraint_name = tc.constraint_name AND kcu.table_name = tc.table_name
        WHERE tc.table_name = 'term_versions_cat'
          AND tc.constraint_type = 'FOREIGN KEY'
          AND kcu.column_name = 'published_by_id'
    ) THEN
        ALTER TABLE term_versions_cat
            ADD CONSTRAINT fk_term_versions_published_by
            FOREIGN KEY (published_by_id) REFERENCES users(id);
    END IF;
END $$;

ALTER TABLE term_versions_cat DROP CONSTRAINT IF EXISTS term_versions_cat_status_check;
ALTER TABLE term_versions_cat
    ADD CONSTRAINT term_versions_cat_status_check
    CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE'));

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'term_versions_cat' AND column_name = 'legal_text'
    ) THEN
        ALTER TABLE term_versions_cat ALTER COLUMN legal_text DROP NOT NULL;

        UPDATE term_versions_cat
        SET document_url = legal_text
        WHERE document_url IS NULL
          AND legal_text LIKE 'https://%'
          AND legal_text NOT LIKE 'https://example.local/%';
    END IF;
END $$;

-- Proveedor: texto vigente que mostraba SelectDocuments.jsx.
UPDATE term_versions_cat v
SET title = 'Términos y Condiciones aplicables al Servicio Bancario para la Gestión y Anticipo de Pago a Proveedores',
    acceptance_text = 'Declaro que he leído, comprendido y acepto irrevocablemente estos Términos y Condiciones aplicables al Servicio Bancario para la Gestión y Anticipo de Pago a Proveedores.',
    content = $md$Al continuar con esta operación, usted (en adelante, "el Proveedor") reconoce, declara y acepta de manera expresa e irrevocable los siguientes Términos y Condiciones aplicables al Servicio Bancario para la Gestión de pago o Anticipo de Pago a Proveedores (en adelante, "Servicio de Anticipo de Pago"), solicitado a través de este sistema (en adelante, "la Plataforma"), brindado por Banco Davivienda Salvadoreño, Sociedad Anónima (en adelante, el "Banco").

Para efectos de estos términos, se entenderá por "Cliente Pagador" la persona natural o jurídica a cuyo cargo fue emitida la cuenta por cobrar (factura, comprobante de crédito fiscal (CCF), DTE y/o cualquier otro documento tributario), en adelante "Cuentas por Cobrar" respecto de las cuales el Proveedor puede optar voluntariamente por solicitar el anticipo de pago:

**1. Visualización y solicitud voluntaria.** El Proveedor reconoce que, al acceder a la Plataforma, podrá visualizar las Cuentas por Cobrar registradas a su favor por el Cliente Pagador, y que tendrá la opción de solicitar, de manera voluntaria, el Servicio de Anticipo de Pago sobre dichas Cuentas por Cobrar.

**2. Naturaleza del pago anticipado.** En caso de optar por la solicitud del Servicio de Anticipo de Pago de alguna de las Cuentas por Cobrar registradas por el Cliente Pagador a su favor, el Proveedor reconoce y acepta que, el pago anticipado que ejecuta el banco se realiza por cuenta, orden y a cargo del Cliente Pagador, entendiéndose por tal la persona natural o jurídica a cuyo cargo fue emitida la Cuenta por Cobrar respecto de la cual se solicita el anticipo, todo ello en ejecución del mandato de anticipo de pago otorgado por dicho Cliente Pagador al Banco.

**3. Validez y exigibilidad de las Cuentas por Cobrar.** El Proveedor declara que las Cuentas por Cobrar respecto de las cuales solicite el Servicio de Anticipo de Pago:

- a. corresponden a obligaciones válidas, exigibles y no controvertidas frente al Cliente Pagador;
- b. no se encuentran sujetas a reclamaciones, disputas, compensaciones, devoluciones, anulaciones ni cualquier otra circunstancia que pueda afectar su existencia, exigibilidad o monto; y
- c. no han sido total ni parcialmente saldadas con anterioridad.

**4. Titularidad y libre disposición de las Cuentas por Cobrar.** El Proveedor declara además que las Cuentas por Cobrar respecto de las cuales solicite el Servicio de Anticipo de Pago:

- a. son de titularidad legítima y exclusiva del Proveedor; y
- b. se encuentran libres de gravámenes, retenciones, cesiones o transferencias previas a terceros, y no se encuentran sujetas a limitaciones de disposición de ninguna naturaleza.

**5. No sometimiento a disputas posteriormente a la solicitud del Servicio de Anticipo de Pago.** Una vez solicitado el Servicio de Anticipo de Pago de una Cuenta por Cobrar a favor del Proveedor y ejecutado el pago anticipado por el Banco, el Proveedor se compromete a no someter dichas Cuentas por Cobrar a compensación, reclamo, disputa comercial o judicial.

**6. Conservación de la relación comercial.** El Proveedor acepta que el pago anticipado ejecutado por el Banco en virtud del Servicio de Anticipo de Pago, no constituye cesión de créditos y que el Banco no adquiere la titularidad de las Cuentas por Cobrar ni se convierte en su cesionario, manteniéndose íntegra la relación jurídica existente entre el Proveedor y el Cliente Pagador. El Proveedor reconoce que la operación únicamente genera a favor del Banco un derecho de reembolso frente al Cliente Pagador por los montos desembolsados en su nombre.

**7. Comisión e Intereses por el servicio.** El Proveedor reconoce y acepta de las Cuentas por Cobrar de las cuales solicite el Servicio de Anticipo de Pago, el Banco ejecutará el pago anticipado por la totalidad del importe de cada Cuenta por Cobrar; no obstante, reconoce y acepta pagar al Banco una comisión como remuneración por la gestión y ejecución del Servicio de Anticipo de Pago, la cual se devengará y será exigible al momento en que el Banco efectúe el desembolso del anticipo, y será cobrada por el Banco de forma separada, conforme a los mecanismos operativos que éste determine, los cuales el proveedor verá reflejado en la plataforma previo al envío de solicitud de pago.

El interés que generará el anticipo de las cuentas por pagar será del {{TASA_INTERES}} POR CIENTO y podrá ajustarse de manera quincenal a opción el Banco los días: uno y quince de cada uno de los meses comprendidos dentro del plazo y también de conformidad a la tasa de referencia que el banco mensualmente publica. La tasa de referencia correspondiente a este mes es del {{TASA_REFERENCIA}} por ciento, la que en sus publicaciones podrá ajustarse a opción del Banco; y el diferencial máximo que el banco podrá aplicar a este crédito durante toda su vigencia y mientras existan saldos pendientes será de {{PUNTOS_PORCENTUALES}} puntos porcentuales arriba de la tasa de referencia vigente a la fecha de cada modificación.

**8. Consentimiento.** El Proveedor reconoce que la aceptación de estos Términos y Condiciones aplicables al Servicio Bancario para la Gestión y Anticipo de Pago a Proveedores y las solicitudes del Servicio de Anticipo de Pago de alguna de las Cuentas por Cobrar registradas a su favor que realice a través de la Plataforma, constituyen una manifestación expresa de su consentimiento.

**9. Limitación de responsabilidad del Banco.** El Proveedor acepta que en le ejecución del Servicio de Anticipo de Pago, el Banco actúa exclusivamente como mandatario del Cliente Pagador y que el Banco no asume responsabilidad alguna por la relación comercial entre el Proveedor y el Cliente Pagador, ni por reclamos, disputas o incumplimientos que pudieren surgir entre ellos.

**10. Legislación Aplicable y Jurisdicción.** Para todos los efectos legales, que se puedan originar del Servicio de Anticipo de Pago, el Proveedor manifiesta que se regirán por las leyes de la República de El Salvador. Para cualquier controversia, las partes se someten a la jurisdicción de los tribunales competentes del distrito de San Salvador, municipio de San Salvador Centro, departamento de San Salvador.$md$
FROM term_types_cat t
WHERE t.id = v.term_type_id
  AND t.unique_code = 'SUPPLIER_TERM_TYPE'
  AND v.status = 'ACTIVE'
  AND v.content IS NULL;

-- Pagador: la versión vigente guarda el texto que se le mostraba y se reemplaza por la 1.1 provisional.
DO $$
DECLARE
    payer_type_id uuid;
    legacy_id     uuid;
BEGIN
    SELECT id INTO payer_type_id FROM term_types_cat WHERE unique_code = 'PAYER_TERM_TYPE';

    SELECT id INTO legacy_id
    FROM term_versions_cat
    WHERE term_type_id = payer_type_id AND status = 'ACTIVE' AND content IS NULL
    ORDER BY created_at DESC
    LIMIT 1;

    IF legacy_id IS NULL THEN
        RETURN;
    END IF;

    UPDATE term_versions_cat
    SET title = 'Términos y Condiciones aplicables al Servicio Bancario para la Gestión y Anticipo de Pago a Proveedores',
        acceptance_text = 'Declaro que he leído, comprendido y acepto irrevocablemente estos Términos y Condiciones aplicables al Servicio Bancario para la Gestión y Anticipo de Pago a Proveedores.',
        content = $md$Al continuar con esta operación, usted (en adelante, "el Proveedor") reconoce, declara y acepta de manera expresa e irrevocable los siguiente Términos y Condiciones aplicables al Servicio Bancario para la Gestión de pago o Anticipo de Pago a Proveedores (en adelante, "Servicio de Anticipo de Pago"), solicitado a través de este sistema (en adelante, "la Plataforma"), brindado por Banco Davivienda Salvadoreño, Sociedad Anónima (en adelante, el "Banco").

**1. Visualización y solicitud voluntaria.** El Proveedor reconoce que, al acceder a la Plataforma, podrá visualizar las Cuentas por Cobrar registradas a su favor por el Cliente Pagador, y que tendrá la opción de solicitar, de manera voluntaria, el Servicio de Anticipo de Pago sobre dichas Cuentas por Cobrar.

**2. Naturaleza del pago anticipado.** En caso de optar por la solicitud del Servicio de Anticipo de Pago de alguna de las Cuentas por Cobrar registradas por el Cliente Pagador a su favor, el Proveedor reconoce y acepta que, el pago anticipado que ejecuta el banco se realiza por cuenta, orden y a cargo del Cliente Pagador.

**3. Validez y exigibilidad de las Cuentas por Cobrar.** El Proveedor declara que las Cuentas por Cobrar respecto de las cuales solicite el Servicio de Anticipo de Pago corresponden a obligaciones válidas, exigibles y no controvertidas frente al Cliente Pagador.

**4. Titularidad y libre disposición de las Cuentas por Cobrar.** El Proveedor declara además que las Cuentas por Cobrar respecto de las cuales solicite el Servicio de Anticipo de Pago son de titularidad legítima y exclusiva del Proveedor.

**5. No sometimiento a disputas posteriormente a la solicitud del Servicio de Anticipo de Pago.** Una vez solicitado el Servicio de Anticipo de Pago de una Cuenta por Cobrar a favor del Proveedor y ejecutado el pago anticipado por el Banco, el Proveedor se compromete a no someter dichas Cuentas por Cobrar a compensación, reclamo, disputa comercial o judicial.

**6. Conservación de la relación comercial.** El Proveedor acepta que el pago anticipado ejecutado por el Banco en virtud del Servicio de Anticipo de Pago, no constituye cesión de créditos y que el Banco no adquiere la titularidad de las Cuentas por Cobrar ni se convierte en su cesionario.$md$,
        status = 'INACTIVE'
    WHERE id = legacy_id;

    INSERT INTO term_versions_cat (
        id, term_type_id, version_number, title, content, acceptance_text,
        status, publication_date, created_at, updated_at
    ) VALUES (
        gen_random_uuid(),
        payer_type_id,
        '1.1',
        'Términos y Condiciones aplicables al Cliente Pagador para la Carga de Cuentas por Pagar',
        $md$> **TEXTO PROVISIONAL.** Este contenido es temporal y debe ser reemplazado por la versión aprobada por el área legal del Banco.

Al cargar el archivo de cuentas por pagar en este sistema (en adelante, "la Plataforma"), usted, en nombre y representación de la entidad a la que pertenece (en adelante, el "Cliente Pagador"), reconoce y acepta los siguientes Términos y Condiciones frente a Banco Davivienda Salvadoreño, Sociedad Anónima (en adelante, el "Banco").

**1. Veracidad de la información.** El Cliente Pagador declara que los documentos registrados (facturas, comprobantes de crédito fiscal y demás documentos tributarios electrónicos) corresponden a obligaciones reales, válidas y exigibles a su cargo, y que los datos del archivo son verídicos y completos.

**2. Mandato de pago anticipado.** El Cliente Pagador autoriza al Banco a pagar anticipadamente a sus proveedores, por su cuenta y orden, las cuentas registradas que estos soliciten, conforme al convenio suscrito con el Banco.

**3. Obligación de reembolso.** El Cliente Pagador se obliga a reembolsar al Banco los montos desembolsados en su nombre en las fechas de vencimiento de cada documento, junto con los cargos pactados en el convenio.

**4. No disposición posterior.** El Cliente Pagador se compromete a no pagar directamente al proveedor, compensar, anular ni modificar los documentos que hayan sido anticipados por el Banco.

**5. Legislación aplicable.** Estos términos se rigen por las leyes de la República de El Salvador.$md$,
        'Declaro que la información cargada es verídica y acepto estos Términos y Condiciones en nombre del Cliente Pagador.',
        'ACTIVE',
        CURRENT_DATE,
        NOW(), NOW()
    );
END $$;

-- Versiones antiguas sin contenido (no debería haber en ambientes nuevos).
UPDATE term_versions_cat v
SET title = COALESCE(v.title, t.term_name),
    acceptance_text = COALESCE(v.acceptance_text, 'Declaro que he leído y acepto estos Términos y Condiciones.'),
    content = 'Versión registrada antes de que el texto se administrara en la plataforma; no se conserva su contenido.'
FROM term_types_cat t
WHERE t.id = v.term_type_id
  AND v.content IS NULL;

-- El archivo puede guardarse con CRLF; el backend normaliza a LF y el hash debe coincidir.
UPDATE term_versions_cat
SET content = replace(content, E'\r', ''),
    title = replace(title, E'\r', ''),
    acceptance_text = replace(acceptance_text, E'\r', '')
WHERE content_hash IS NULL
  AND (content LIKE E'%\r%' OR title LIKE E'%\r%' OR acceptance_text LIKE E'%\r%');

UPDATE term_versions_cat
SET content_hash = encode(sha256(convert_to(title || E'\n\n' || content || E'\n\n' || acceptance_text, 'UTF8')), 'hex')
WHERE status <> 'DRAFT'
  AND content_hash IS NULL;

UPDATE term_versions_cat
SET publication_date = created_at::date
WHERE status <> 'DRAFT'
  AND publication_date IS NULL;

ALTER TABLE term_versions_cat ALTER COLUMN title           SET NOT NULL;
ALTER TABLE term_versions_cat ALTER COLUMN content         SET NOT NULL;
ALTER TABLE term_versions_cat ALTER COLUMN acceptance_text SET NOT NULL;

ALTER TABLE term_versions_cat DROP CONSTRAINT IF EXISTS ck_term_versions_published_fields;
ALTER TABLE term_versions_cat
    ADD CONSTRAINT ck_term_versions_published_fields
    CHECK (status = 'DRAFT' OR (content_hash IS NOT NULL AND publication_date IS NOT NULL));

CREATE UNIQUE INDEX IF NOT EXISTS ux_term_versions_one_active
    ON term_versions_cat (term_type_id)
    WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX IF NOT EXISTS ux_term_versions_type_version
    ON term_versions_cat (term_type_id, lower(version_number));

ALTER TABLE term_versions_cat DROP COLUMN IF EXISTS legal_text;

SELECT t.unique_code, v.version_number, v.status, v.publication_date,
       left(v.content_hash, 12) AS hash, length(v.content) AS chars,
       (SELECT count(*) FROM acceptance_audits a WHERE a.version_id = v.id) AS acceptances
FROM term_versions_cat v
JOIN term_types_cat t ON t.id = v.term_type_id
ORDER BY t.unique_code, v.created_at;

COMMIT;
