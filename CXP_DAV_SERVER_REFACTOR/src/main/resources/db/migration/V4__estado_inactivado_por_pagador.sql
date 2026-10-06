-- Separa los documentos inactivos en dos estados:
--   IN_QUARANTINE         el sistema los retiró por no financiables (vencidos o dentro de los días de gracia).
--   INACTIVATED_BY_PAYER  el pagador los pasó manualmente a Inactivo.

ALTER TABLE public.documents DROP CONSTRAINT documents_status_check;
ALTER TABLE public.documents ADD CONSTRAINT documents_status_check CHECK (status IN (
    'APPROVED', 'REQUESTED_FOR_FINANCING', 'REQUESTED_FOR_DISBURSEMENT', 'DISBURSED',
    'IN_QUARANTINE', 'INACTIVATED_BY_PAYER'));

ALTER TABLE public.document_logs DROP CONSTRAINT document_logs_status_check;
ALTER TABLE public.document_logs ADD CONSTRAINT document_logs_status_check CHECK (status IN (
    'APPROVED', 'REQUESTED_FOR_FINANCING', 'REQUESTED_FOR_DISBURSEMENT', 'DISBURSED',
    'IN_QUARANTINE', 'INACTIVATED_BY_PAYER'));

-- Los documentos ya inactivos se reclasifican según quién los inactivó: si el registro
-- de la bitácora es de un usuario con rol PAYER, fue la inactivación manual del pagador.
WITH payer_inactivations AS (
    SELECT l.id AS log_id, l.document_id
    FROM public.document_logs l
    JOIN public.documents d ON d.id = l.document_id
    JOIN public.users u ON u.id = l.user_id
    JOIN public.roles_cat r ON r.id = u.role_id
    WHERE l.status = 'IN_QUARANTINE'
      AND d.status = 'IN_QUARANTINE'
      AND r.name = 'PAYER'
),
updated_logs AS (
    UPDATE public.document_logs l
    SET status = 'INACTIVATED_BY_PAYER'
    FROM payer_inactivations p
    WHERE l.id = p.log_id
)
UPDATE public.documents d
SET status = 'INACTIVATED_BY_PAYER'
FROM payer_inactivations p
WHERE d.id = p.document_id;
