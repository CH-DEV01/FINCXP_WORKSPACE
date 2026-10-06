-- Esquema completo de la base de Financiamiento de Cuentas por Pagar (versión final del refactor).
-- Incluye tablas, llaves, restricciones de enums e índices únicos (doble fondeo, términos).
-- En bases que ya existían antes de Flyway esta migración no se ejecuta (baseline).

CREATE TABLE public.acceptance_audits (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    user_agent character varying(255) NOT NULL,
    version_id uuid NOT NULL,
    user_id uuid NOT NULL
);

CREATE TABLE public.bank_accounts (
    id uuid NOT NULL,
    account_number character varying(50) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    is_main boolean NOT NULL,
    status character varying(12) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    entity_id uuid NOT NULL,
    CONSTRAINT bank_accounts_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);

CREATE TABLE public.bank_holidays_cat (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    description character varying(255) NOT NULL,
    holiday_date date NOT NULL,
    status character varying(12) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT bank_holidays_cat_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);

CREATE TABLE public.credit_facilities (
    id uuid NOT NULL,
    amount_in_use numeric(19,4) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    credit_facility_number character varying(255) NOT NULL,
    facility_limit_amount numeric(19,4) NOT NULL,
    status character varying(50) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    warning_threshold_percentage numeric(5,2),
    payer_id uuid NOT NULL,
    CONSTRAINT credit_facilities_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);

CREATE TABLE public.credit_facility_histories (
    id uuid NOT NULL,
    amount numeric(19,4) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    reference_number character varying(255) NOT NULL,
    repayment_type character varying(50) NOT NULL,
    credit_facility_id uuid NOT NULL,
    executed_by_id uuid NOT NULL,
    payer_id uuid NOT NULL,
    CONSTRAINT credit_facility_histories_repayment_type_check CHECK (((repayment_type)::text = ANY ((ARRAY['PARTIAL'::character varying, 'FULL'::character varying, 'INITIAL_BALANCE'::character varying, 'DOCUMENT_INACTIVATION'::character varying])::text[])))
);

CREATE TABLE public.disbursement_batches (
    id uuid NOT NULL,
    batch_number character varying(255) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    output_file_name character varying(255) NOT NULL,
    status character varying(50) NOT NULL,
    total_amount numeric(19,4) NOT NULL,
    total_commission numeric(19,4) NOT NULL,
    total_interest numeric(19,4) NOT NULL,
    transaction_count integer NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    confirmed_by_id uuid,
    created_by_id uuid NOT NULL,
    overdue_review_note character varying(1000),
    due_date date,
    request_date date,
    payer_id uuid,
    disbursement_date date,
    CONSTRAINT disbursement_batches_status_check CHECK (((status)::text = ANY ((ARRAY['CREATED'::character varying, 'PROCESSING'::character varying, 'SETTLED'::character varying, 'FAILED'::character varying, 'PARTIALLY_FAILED'::character varying])::text[])))
);

CREATE TABLE public.disbursement_policies_cat (
    id uuid NOT NULL,
    code character varying(255) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    description character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    status character varying(50) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    offset_days integer,
    type character varying(30),
    weekdays character varying(100),
    CONSTRAINT disbursement_policies_cat_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[]))),
    CONSTRAINT disbursement_policies_cat_type_check CHECK (((type)::text = ANY ((ARRAY['T_PLUS_N'::character varying, 'WEEKDAYS'::character varying])::text[])))
);

CREATE TABLE public.document_logs (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    status character varying(50),
    acceptance_audit_id uuid,
    document_id uuid NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT document_logs_status_check CHECK (((status)::text = ANY ((ARRAY['APPROVED'::character varying, 'REQUESTED_FOR_FINANCING'::character varying, 'REQUESTED_FOR_DISBURSEMENT'::character varying, 'DISBURSED'::character varying, 'IN_QUARANTINE'::character varying])::text[])))
);

CREATE TABLE public.documents (
    id uuid NOT NULL,
    control_number character varying(31),
    created_at timestamp(6) with time zone NOT NULL,
    document_number character varying(255),
    due_date date NOT NULL,
    generation_code character varying(36),
    invoice_type character varying(30),
    issuance_method character varying(30),
    issue_date date NOT NULL,
    nominal_amount numeric(19,4) NOT NULL,
    received_stamp character varying(40),
    status character varying(50) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    master_agreement_id uuid NOT NULL,
    upload_batch_id uuid NOT NULL,
    quarantine_reason character varying(50),
    quarantined_at timestamp(6) with time zone,
    CONSTRAINT documents_invoice_type_check CHECK (((invoice_type)::text = ANY ((ARRAY['CCF'::character varying, 'FCI'::character varying])::text[]))),
    CONSTRAINT documents_issuance_method_check CHECK (((issuance_method)::text = ANY ((ARRAY['DIGITAL'::character varying, 'PAPER'::character varying])::text[]))),
    CONSTRAINT documents_quarantine_reason_check CHECK (((quarantine_reason)::text = ANY ((ARRAY['DUE_DATE_EXPIRED'::character varying, 'NEAR_DUE_DATE_ON_REQUEST'::character varying, 'NEAR_DUE_DATE_UNREQUESTED'::character varying, 'DISBURSEMENT_FAILED'::character varying, 'MANUAL_REVIEW'::character varying])::text[]))),
    CONSTRAINT documents_status_check CHECK (((status)::text = ANY ((ARRAY['APPROVED'::character varying, 'REQUESTED_FOR_FINANCING'::character varying, 'REQUESTED_FOR_DISBURSEMENT'::character varying, 'DISBURSED'::character varying, 'IN_QUARANTINE'::character varying])::text[])))
);

CREATE TABLE public.entities (
    id uuid NOT NULL,
    code character varying(25) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    name character varying(255) NOT NULL,
    nit character varying(25) NOT NULL,
    niu character varying(25) NOT NULL,
    status character varying(50) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    entity_type_id uuid NOT NULL,
    CONSTRAINT entities_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);

CREATE TABLE public.entity_types_cat (
    id uuid NOT NULL,
    code character varying(255) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    name character varying(255) NOT NULL,
    status character varying(12) NOT NULL,
    updated_at timestamp(6) with time zone,
    CONSTRAINT entity_types_cat_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);

CREATE TABLE public.excel_template_columns (
    id uuid NOT NULL,
    is_active boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    excel_column_name character varying(100) NOT NULL,
    logical_dto_field character varying(100) NOT NULL,
    is_required boolean NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL
);

CREATE TABLE public.financing_requests (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    request_number character varying(50) NOT NULL,
    status character varying(50) NOT NULL,
    total_net_amount numeric(19,4) NOT NULL,
    total_amount_to_finance numeric(19,4) NOT NULL,
    total_flat_amount numeric(19,4) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    requested_by_id uuid NOT NULL,
    supplier_id uuid NOT NULL,
    CONSTRAINT financing_requests_status_check CHECK (((status)::text = ANY ((ARRAY['SUBMITTED'::character varying, 'COMPLETED'::character varying])::text[])))
);

CREATE TABLE public.financing_transactions (
    id uuid NOT NULL,
    amount_to_be_disbursed numeric(19,4) NOT NULL,
    amount_to_finance numeric(19,4) NOT NULL,
    commission_amount numeric(19,4) NOT NULL,
    created_at date NOT NULL,
    discount_rate numeric(19,6) NOT NULL,
    financing_percentage numeric(19,6) NOT NULL,
    flat_amount numeric(19,4) NOT NULL,
    interest_amount numeric(19,4) NOT NULL,
    iva_amount numeric(19,4) NOT NULL,
    scheduled_disbursement_date date NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    disbursement_batch_id uuid,
    document_id uuid NOT NULL,
    financing_request_id uuid NOT NULL
);

CREATE TABLE public.master_agreements (
    id uuid NOT NULL,
    agreement_type character varying(50) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    status character varying(50) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    disbursement_policy_id uuid NOT NULL,
    payer_id uuid NOT NULL,
    payment_policy_id uuid NOT NULL,
    supplier_id uuid NOT NULL,
    CONSTRAINT master_agreements_agreement_type_check CHECK (((agreement_type)::text = ANY ((ARRAY['STANDARD'::character varying, 'RECOURSE'::character varying, 'NON_RECOURSE'::character varying, 'INVERSE'::character varying, 'SCF'::character varying])::text[]))),
    CONSTRAINT master_agreements_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);

CREATE TABLE public.menus_cat (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    description character varying(255),
    icon character varying(50),
    label character varying(100) NOT NULL,
    path character varying(150) NOT NULL,
    status character varying(12) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT menus_cat_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);

CREATE TABLE public.payment_policies_cat (
    id uuid NOT NULL,
    code character varying(255) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    days_count integer NOT NULL,
    description character varying(255) NOT NULL,
    status character varying(50) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT payment_policies_cat_days_count_check CHECK ((days_count >= 1)),
    CONSTRAINT payment_policies_cat_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);

CREATE TABLE public.product_pricing_terms (
    id uuid NOT NULL,
    calculation_base character varying(50) NOT NULL,
    commission_rate numeric(19,6) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    interest_rate numeric(19,6) NOT NULL,
    status character varying(50) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    credit_facility_id uuid NOT NULL,
    CONSTRAINT product_pricing_terms_calculation_base_check CHECK (((calculation_base)::text = ANY ((ARRAY['COMERCIAL_360'::character varying, 'CALENDARIO_365'::character varying])::text[]))),
    CONSTRAINT product_pricing_terms_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);

CREATE TABLE public.role_menus (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    display_order integer NOT NULL,
    menu_id uuid NOT NULL,
    role_id uuid NOT NULL
);

CREATE TABLE public.role_routes (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    is_index boolean NOT NULL,
    role_id uuid NOT NULL,
    route_id uuid NOT NULL
);

CREATE TABLE public.roles_cat (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    description character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    status character varying(12) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    default_route character varying(100),
    CONSTRAINT roles_cat_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);

CREATE TABLE public.routes_cat (
    id uuid NOT NULL,
    component_name character varying(100) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    description character varying(255),
    path character varying(150) NOT NULL,
    status character varying(12) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT routes_cat_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);

CREATE TABLE public.system_parameters (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    param_key character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    param_value character varying(255) NOT NULL
);

CREATE TABLE public.term_types_cat (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    status character varying(50) NOT NULL,
    term_name character varying(255) NOT NULL,
    unique_code character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT term_types_cat_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);

CREATE TABLE public.term_versions_cat (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    publication_date date,
    status character varying(50) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    version_number character varying(255) NOT NULL,
    term_type_id uuid NOT NULL,
    content_hash character varying(64),
    document_url character varying(500),
    published_by_id uuid,
    title character varying(255) NOT NULL,
    content text NOT NULL,
    acceptance_text character varying(500) NOT NULL,
    CONSTRAINT ck_term_versions_published_fields CHECK ((((status)::text = 'DRAFT'::text) OR ((content_hash IS NOT NULL) AND (publication_date IS NOT NULL)))),
    CONSTRAINT term_versions_cat_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);

CREATE TABLE public.upload_batches (
    id uuid NOT NULL,
    batch_number character varying(255) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    file_name character varying(255) NOT NULL,
    status character varying(50) NOT NULL,
    total_records integer NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    uploaded_and_approved_by_id uuid NOT NULL,
    CONSTRAINT upload_batches_status_check CHECK (((status)::text = ANY ((ARRAY['PROCESSING'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying, 'PARTIALLY_FAILED'::character varying])::text[]))),
    CONSTRAINT upload_batches_total_records_check CHECK ((total_records >= 0))
);

CREATE TABLE public.users (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    dui character varying(15) NOT NULL,
    email character varying(255) NOT NULL,
    first_name character varying(255) NOT NULL,
    last_name character varying(255) NOT NULL,
    status character varying(50) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    entity_id uuid NOT NULL,
    role_id uuid NOT NULL,
    CONSTRAINT users_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);

ALTER TABLE ONLY public.acceptance_audits
    ADD CONSTRAINT acceptance_audits_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.bank_accounts
    ADD CONSTRAINT bank_accounts_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.bank_holidays_cat
    ADD CONSTRAINT bank_holidays_cat_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.credit_facilities
    ADD CONSTRAINT credit_facilities_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.credit_facility_histories
    ADD CONSTRAINT credit_facility_histories_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.disbursement_batches
    ADD CONSTRAINT disbursement_batches_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.disbursement_policies_cat
    ADD CONSTRAINT disbursement_policies_cat_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.document_logs
    ADD CONSTRAINT document_logs_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT documents_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.entities
    ADD CONSTRAINT entities_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.entity_types_cat
    ADD CONSTRAINT entity_types_cat_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.excel_template_columns
    ADD CONSTRAINT excel_template_columns_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.financing_requests
    ADD CONSTRAINT financing_requests_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.financing_transactions
    ADD CONSTRAINT financing_transactions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.master_agreements
    ADD CONSTRAINT master_agreements_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.menus_cat
    ADD CONSTRAINT menus_cat_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.payment_policies_cat
    ADD CONSTRAINT payment_policies_cat_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.product_pricing_terms
    ADD CONSTRAINT product_pricing_terms_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.role_menus
    ADD CONSTRAINT role_menus_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.role_routes
    ADD CONSTRAINT role_routes_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.roles_cat
    ADD CONSTRAINT roles_cat_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.routes_cat
    ADD CONSTRAINT routes_cat_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.system_parameters
    ADD CONSTRAINT system_parameters_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.term_types_cat
    ADD CONSTRAINT term_types_cat_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.term_versions_cat
    ADD CONSTRAINT term_versions_cat_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.entities
    ADD CONSTRAINT uk3pvt8gndav34x92d4io543ol2 UNIQUE (niu);

ALTER TABLE ONLY public.entities
    ADD CONSTRAINT uk5qu23ja9ixrm2nfbqa7xl3wab UNIQUE (code);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);

ALTER TABLE ONLY public.payment_policies_cat
    ADD CONSTRAINT uk9wps610udqb4jf0y5xum1kvrm UNIQUE (code);

ALTER TABLE ONLY public.menus_cat
    ADD CONSTRAINT uk_menus_cat_path UNIQUE (path);

ALTER TABLE ONLY public.master_agreements
    ADD CONSTRAINT uk_payer_supplier_agreement UNIQUE (payer_id, supplier_id, agreement_type);

ALTER TABLE ONLY public.role_menus
    ADD CONSTRAINT uk_role_menus_role_menu UNIQUE (role_id, menu_id);

ALTER TABLE ONLY public.role_routes
    ADD CONSTRAINT uk_role_routes_role_route UNIQUE (role_id, route_id);

ALTER TABLE ONLY public.routes_cat
    ADD CONSTRAINT uk_routes_cat_path_component UNIQUE (path, component_name);

ALTER TABLE ONLY public.financing_transactions
    ADD CONSTRAINT ukaem49rxghrlrd5ehpd54jykmw UNIQUE (document_id);

ALTER TABLE ONLY public.disbursement_batches
    ADD CONSTRAINT ukayc99yhffeys2q988ku5wp5bb UNIQUE (batch_number);

ALTER TABLE ONLY public.disbursement_policies_cat
    ADD CONSTRAINT ukbitukkd00r3mmrl81j3e52hp9 UNIQUE (code);

ALTER TABLE ONLY public.bank_holidays_cat
    ADD CONSTRAINT ukcesrthtnmj9r6l5lsjfeeuksx UNIQUE (holiday_date);

ALTER TABLE ONLY public.financing_requests
    ADD CONSTRAINT ukj6nsgl1h7gmvf1ew9xsnrnyuk UNIQUE (request_number);

ALTER TABLE ONLY public.system_parameters
    ADD CONSTRAINT ukjl6v8jrdwppjo5hjixnbqou1n UNIQUE (param_key);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT ukn00icka5w2gxjyo0tlgyau168 UNIQUE (dui);

ALTER TABLE ONLY public.roles_cat
    ADD CONSTRAINT ukoj319sfqub7mbtmklu7alws7m UNIQUE (name);

ALTER TABLE ONLY public.upload_batches
    ADD CONSTRAINT ukoq0kvrd9x3jack14bu5rx0mjj UNIQUE (batch_number);

ALTER TABLE ONLY public.entity_types_cat
    ADD CONSTRAINT ukqa96cmb4a7grwp2xm89mg9nfk UNIQUE (code);

ALTER TABLE ONLY public.bank_accounts
    ADD CONSTRAINT ukr9gi1et82prjsig51uqxj2qm6 UNIQUE (account_number);

ALTER TABLE ONLY public.entities
    ADD CONSTRAINT ukrh9g82kd3mu9d7ncd5r451aiu UNIQUE (nit);

ALTER TABLE ONLY public.upload_batches
    ADD CONSTRAINT upload_batches_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

CREATE UNIQUE INDEX uq_documents_control_number ON public.documents USING btree (upper((control_number)::text)) WHERE (control_number IS NOT NULL);

CREATE UNIQUE INDEX uq_documents_generation_code ON public.documents USING btree (upper((generation_code)::text)) WHERE (generation_code IS NOT NULL);

CREATE UNIQUE INDEX uq_documents_paper_number_year ON public.documents USING btree (master_agreement_id, document_number, EXTRACT(year FROM issue_date)) WHERE (((issuance_method)::text = 'PAPER'::text) AND (document_number IS NOT NULL));

CREATE UNIQUE INDEX uq_documents_received_stamp ON public.documents USING btree (upper((received_stamp)::text)) WHERE (received_stamp IS NOT NULL);

CREATE UNIQUE INDEX ux_term_versions_one_active ON public.term_versions_cat USING btree (term_type_id) WHERE ((status)::text = 'ACTIVE'::text);

CREATE UNIQUE INDEX ux_term_versions_type_version ON public.term_versions_cat USING btree (term_type_id, lower((version_number)::text));

ALTER TABLE ONLY public.financing_transactions
    ADD CONSTRAINT fk180g6wnayr0rp2sovx26flvfn FOREIGN KEY (document_id) REFERENCES public.documents(id);

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT fk44037dsh2i14fw0yic9k5wi6e FOREIGN KEY (upload_batch_id) REFERENCES public.upload_batches(id);

ALTER TABLE ONLY public.term_versions_cat
    ADD CONSTRAINT fk4phfxiri6unxw5ntu9xg5eg0q FOREIGN KEY (term_type_id) REFERENCES public.term_types_cat(id);

ALTER TABLE ONLY public.role_routes
    ADD CONSTRAINT fk67mg2v594g0kt3jpboqqd19w FOREIGN KEY (role_id) REFERENCES public.roles_cat(id);

ALTER TABLE ONLY public.financing_requests
    ADD CONSTRAINT fk6gf2xvvi32hxn8nuv9i2bwknt FOREIGN KEY (supplier_id) REFERENCES public.entities(id);

ALTER TABLE ONLY public.documents
    ADD CONSTRAINT fk8ac26ac37d22ij9aw91ia9n0u FOREIGN KEY (master_agreement_id) REFERENCES public.master_agreements(id);

ALTER TABLE ONLY public.document_logs
    ADD CONSTRAINT fk8m4nodv7pd6x3csnqt91k15ft FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.disbursement_batches
    ADD CONSTRAINT fk99lla94fyhxtmu72hvahu2fqf FOREIGN KEY (payer_id) REFERENCES public.entities(id);

ALTER TABLE ONLY public.role_routes
    ADD CONSTRAINT fk9ga2wwn8a4tqkm5ostskvje25 FOREIGN KEY (route_id) REFERENCES public.routes_cat(id);

ALTER TABLE ONLY public.document_logs
    ADD CONSTRAINT fkbe8xda13pweioqipnts8b94tf FOREIGN KEY (document_id) REFERENCES public.documents(id);

ALTER TABLE ONLY public.financing_requests
    ADD CONSTRAINT fkbitiq6bt0yh8plpwewv2jvvk9 FOREIGN KEY (requested_by_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.master_agreements
    ADD CONSTRAINT fkbptornfp45s8aeryp31c1snn4 FOREIGN KEY (payment_policy_id) REFERENCES public.payment_policies_cat(id);

ALTER TABLE ONLY public.financing_transactions
    ADD CONSTRAINT fkbx1ffoav9jxb6xtwj7cxiwmeg FOREIGN KEY (disbursement_batch_id) REFERENCES public.disbursement_batches(id);

ALTER TABLE ONLY public.entities
    ADD CONSTRAINT fkcmihvjj9w33brbxo83uw28ur8 FOREIGN KEY (entity_type_id) REFERENCES public.entity_types_cat(id);

ALTER TABLE ONLY public.credit_facility_histories
    ADD CONSTRAINT fke34vtup6rk8ni85dv7uj7jdqq FOREIGN KEY (payer_id) REFERENCES public.entities(id);

ALTER TABLE ONLY public.role_menus
    ADD CONSTRAINT fkfjukt7domwo097pqv0dsyg6qc FOREIGN KEY (menu_id) REFERENCES public.menus_cat(id);

ALTER TABLE ONLY public.role_menus
    ADD CONSTRAINT fkfwr94hahmk4a1gsl64xvpjtgu FOREIGN KEY (role_id) REFERENCES public.roles_cat(id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fkgvqnng86rr739m4th9m97x5x8 FOREIGN KEY (role_id) REFERENCES public.roles_cat(id);

ALTER TABLE ONLY public.disbursement_batches
    ADD CONSTRAINT fkhaqe728oex61rsyxlwlqpqu49 FOREIGN KEY (created_by_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fkhb1q1fcncex0uvlxs3h7vjofc FOREIGN KEY (entity_id) REFERENCES public.entities(id);

ALTER TABLE ONLY public.credit_facility_histories
    ADD CONSTRAINT fkhcux15krk9rrb0k4p8d52a4p8 FOREIGN KEY (credit_facility_id) REFERENCES public.credit_facilities(id);

ALTER TABLE ONLY public.master_agreements
    ADD CONSTRAINT fkjb96vsqbd0qb7ggrtkgwn0sm4 FOREIGN KEY (payer_id) REFERENCES public.entities(id);

ALTER TABLE ONLY public.upload_batches
    ADD CONSTRAINT fkjdyad0sl1jshxaa8cr47g213f FOREIGN KEY (uploaded_and_approved_by_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.product_pricing_terms
    ADD CONSTRAINT fkk42ci8lprdjmogy7wg7ppekf1 FOREIGN KEY (credit_facility_id) REFERENCES public.credit_facilities(id);

ALTER TABLE ONLY public.master_agreements
    ADD CONSTRAINT fkkvwnn34h46y2x3wfnanfy4udm FOREIGN KEY (disbursement_policy_id) REFERENCES public.disbursement_policies_cat(id);

ALTER TABLE ONLY public.bank_accounts
    ADD CONSTRAINT fkkxgaj0oqy0rtgfr5hu21gg2n9 FOREIGN KEY (entity_id) REFERENCES public.entities(id);

ALTER TABLE ONLY public.master_agreements
    ADD CONSTRAINT fklavsu9mm469nx6r0xwvawmqgf FOREIGN KEY (supplier_id) REFERENCES public.entities(id);

ALTER TABLE ONLY public.credit_facility_histories
    ADD CONSTRAINT fklrl79sitxigx8j84ep2hm6dg FOREIGN KEY (executed_by_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.financing_transactions
    ADD CONSTRAINT fklsduiyge3burtolj7g62pwqjv FOREIGN KEY (financing_request_id) REFERENCES public.financing_requests(id);

ALTER TABLE ONLY public.term_versions_cat
    ADD CONSTRAINT fkmjeid3sy24lor2ykpmhnmvqgg FOREIGN KEY (published_by_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.disbursement_batches
    ADD CONSTRAINT fkog4dxrdp2miu0lnyypfpimkqf FOREIGN KEY (confirmed_by_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.document_logs
    ADD CONSTRAINT fkoillojcc6m8a273ua4gkf70ch FOREIGN KEY (acceptance_audit_id) REFERENCES public.acceptance_audits(id);

ALTER TABLE ONLY public.credit_facilities
    ADD CONSTRAINT fkpt0flnudemxapf1lestaex1sj FOREIGN KEY (payer_id) REFERENCES public.entities(id);

ALTER TABLE ONLY public.acceptance_audits
    ADD CONSTRAINT fkrikupdb70o2epcye2gnxwwd4p FOREIGN KEY (version_id) REFERENCES public.term_versions_cat(id);

ALTER TABLE ONLY public.acceptance_audits
    ADD CONSTRAINT fktpwjmbxonc0qmgiiy1nsw7vi6 FOREIGN KEY (user_id) REFERENCES public.users(id);
