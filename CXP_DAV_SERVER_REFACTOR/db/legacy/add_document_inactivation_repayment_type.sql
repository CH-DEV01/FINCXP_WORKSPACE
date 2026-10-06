-- Permite el tipo DOCUMENT_INACTIVATION en el historial del cupo. Hibernate creó la
-- restricción con los valores del enum de ese momento y ddl-auto=update no la actualiza.
-- Uso:
--   psql -h localhost -U postgres -d postgres -f db/add_document_inactivation_repayment_type.sql

BEGIN;

ALTER TABLE credit_facility_histories
    DROP CONSTRAINT IF EXISTS credit_facility_histories_repayment_type_check;

ALTER TABLE credit_facility_histories
    ADD CONSTRAINT credit_facility_histories_repayment_type_check
    CHECK (repayment_type IN ('PARTIAL', 'FULL', 'INITIAL_BALANCE', 'DOCUMENT_INACTIVATION'));

COMMIT;
