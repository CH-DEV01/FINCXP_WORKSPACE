-- Dispersión de documentos no financiables: el banco paga al proveedor el monto de la
-- factura con cargo a la cuenta del pagador. Los lotes agrupan los documentos de un
-- pagador con la misma fecha de vencimiento.

CREATE TABLE public.dispersion_batches (
    id uuid NOT NULL,
    batch_number character varying(255) NOT NULL,
    payer_id uuid NOT NULL,
    payer_account_number character varying(255) NOT NULL,
    due_date date NOT NULL,
    dispersion_date date NOT NULL,
    document_count integer NOT NULL,
    total_amount numeric(38,18) NOT NULL,
    status character varying(50) NOT NULL,
    signer_id uuid,
    created_by_id uuid NOT NULL,
    confirmed_by_id uuid,
    confirmed_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT dispersion_batches_pkey PRIMARY KEY (id),
    CONSTRAINT uk_dispersion_batches_batch_number UNIQUE (batch_number),
    CONSTRAINT dispersion_batches_status_check CHECK (status IN ('CREATED', 'SETTLED')),
    CONSTRAINT dispersion_batches_document_count_check CHECK (document_count > 0),
    CONSTRAINT fk_dispersion_batches_payer FOREIGN KEY (payer_id) REFERENCES public.entities(id),
    CONSTRAINT fk_dispersion_batches_signer FOREIGN KEY (signer_id) REFERENCES public.users(id),
    CONSTRAINT fk_dispersion_batches_created_by FOREIGN KEY (created_by_id) REFERENCES public.users(id),
    CONSTRAINT fk_dispersion_batches_confirmed_by FOREIGN KEY (confirmed_by_id) REFERENCES public.users(id)
);

CREATE INDEX ix_dispersion_batches_payer_created ON public.dispersion_batches (payer_id, created_at DESC);

ALTER TABLE public.documents ADD COLUMN dispersion_batch_id uuid;
ALTER TABLE public.documents ADD CONSTRAINT fk_documents_dispersion_batch
    FOREIGN KEY (dispersion_batch_id) REFERENCES public.dispersion_batches(id);
CREATE INDEX ix_documents_dispersion_batch ON public.documents (dispersion_batch_id);

ALTER TABLE public.documents DROP CONSTRAINT documents_status_check;
ALTER TABLE public.documents ADD CONSTRAINT documents_status_check CHECK (status IN (
    'APPROVED', 'REQUESTED_FOR_FINANCING', 'REQUESTED_FOR_DISBURSEMENT', 'DISBURSED',
    'IN_QUARANTINE', 'INACTIVATED_BY_PAYER', 'REQUESTED_FOR_DISPERSION', 'DISPERSED'));

ALTER TABLE public.document_logs DROP CONSTRAINT document_logs_status_check;
ALTER TABLE public.document_logs ADD CONSTRAINT document_logs_status_check CHECK (status IN (
    'APPROVED', 'REQUESTED_FOR_FINANCING', 'REQUESTED_FOR_DISBURSEMENT', 'DISBURSED',
    'IN_QUARANTINE', 'INACTIVATED_BY_PAYER', 'REQUESTED_FOR_DISPERSION', 'DISPERSED'));

-- Pantallas del administrador bancario (ADMIN): terminal y bitácora de dispersiones.
INSERT INTO public.routes_cat (id, component_name, created_at, description, path, status, updated_at) VALUES
    ('c5000000-0000-4000-8000-000000000001', 'DispersionTerminal', now(), 'Terminal de dispersiones', 'dispersion-terminal', 'ACTIVE', now()),
    ('c5000000-0000-4000-8000-000000000002', 'DispersionBatchLog', now(), 'Bitácora de dispersiones', 'dispersion-batches-history', 'ACTIVE', now());

INSERT INTO public.role_routes (id, created_at, is_index, role_id, route_id)
SELECT v.id, now(), false, r.id, v.route_id
FROM public.roles_cat r
CROSS JOIN (VALUES
    ('c5000000-0000-4000-8000-000000000011'::uuid, 'c5000000-0000-4000-8000-000000000001'::uuid),
    ('c5000000-0000-4000-8000-000000000012'::uuid, 'c5000000-0000-4000-8000-000000000002'::uuid)) AS v(id, route_id)
WHERE r.name = 'ADMIN';

INSERT INTO public.menus_cat (id, created_at, description, icon, label, path, status, updated_at) VALUES
    ('c5000000-0000-4000-8000-000000000021', now(), NULL, 'send', 'Dispersiones', '/admin/dispersion-terminal', 'ACTIVE', now()),
    ('c5000000-0000-4000-8000-000000000022', now(), NULL, 'clock', 'Bitácora de dispersiones', '/admin/dispersion-batches-history', 'ACTIVE', now());

INSERT INTO public.role_menus (id, created_at, display_order, menu_id, role_id)
SELECT v.id, now(), v.display_order, v.menu_id, r.id
FROM public.roles_cat r
CROSS JOIN (VALUES
    ('c5000000-0000-4000-8000-000000000031'::uuid, 9, 'c5000000-0000-4000-8000-000000000021'::uuid),
    ('c5000000-0000-4000-8000-000000000032'::uuid, 10, 'c5000000-0000-4000-8000-000000000022'::uuid)) AS v(id, display_order, menu_id)
WHERE r.name = 'ADMIN';
