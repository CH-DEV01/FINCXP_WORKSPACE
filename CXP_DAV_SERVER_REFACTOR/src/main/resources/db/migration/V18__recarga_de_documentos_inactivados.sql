-- Un documento inactivado por el pagador se puede volver a cargar: los índices de doble
-- fondeo dejan de considerarlo. Los documentos en cuarentena siguen contando, porque aún
-- pueden dispersarse.

DROP INDEX public.uq_documents_control_number;
DROP INDEX public.uq_documents_generation_code;
DROP INDEX public.uq_documents_paper_number_year;
DROP INDEX public.uq_documents_received_stamp;

CREATE UNIQUE INDEX uq_documents_control_number ON public.documents USING btree (upper((control_number)::text))
    WHERE (control_number IS NOT NULL AND status <> 'INACTIVATED_BY_PAYER');
CREATE UNIQUE INDEX uq_documents_generation_code ON public.documents USING btree (upper((generation_code)::text))
    WHERE (generation_code IS NOT NULL AND status <> 'INACTIVATED_BY_PAYER');
CREATE UNIQUE INDEX uq_documents_paper_number_year ON public.documents USING btree
    (master_agreement_id, document_number, EXTRACT(year FROM issue_date))
    WHERE (issuance_method = 'PAPER' AND document_number IS NOT NULL AND status <> 'INACTIVATED_BY_PAYER');
CREATE UNIQUE INDEX uq_documents_received_stamp ON public.documents USING btree (upper((received_stamp)::text))
    WHERE (received_stamp IS NOT NULL AND status <> 'INACTIVATED_BY_PAYER');
