-- El pago parcial del cupo se registra por lote de desembolso: restablece el valor nominal
-- de los documentos del lote. Cada lote se abona una sola vez.

ALTER TABLE public.credit_facility_histories ADD COLUMN disbursement_batch_id uuid;
ALTER TABLE public.credit_facility_histories ADD CONSTRAINT fk_credit_facility_histories_disbursement_batch
    FOREIGN KEY (disbursement_batch_id) REFERENCES public.disbursement_batches(id);
CREATE UNIQUE INDEX uk_credit_facility_histories_disbursement_batch
    ON public.credit_facility_histories (disbursement_batch_id) WHERE disbursement_batch_id IS NOT NULL;
