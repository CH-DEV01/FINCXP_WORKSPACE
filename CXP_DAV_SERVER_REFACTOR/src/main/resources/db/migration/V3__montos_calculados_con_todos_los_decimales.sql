-- Los montos y factores calculados se guardan con todos los decimales del cálculo
-- (escala 18, ver Money.STORAGE_SCALE); solo se redondean a 2 decimales al mostrarse.
-- Los montos nominales siguen con su escala, porque se cargan con 2 decimales como máximo.

ALTER TABLE public.financing_transactions
    ALTER COLUMN amount_to_be_disbursed TYPE numeric(38,18),
    ALTER COLUMN amount_to_finance TYPE numeric(38,18),
    ALTER COLUMN commission_amount TYPE numeric(38,18),
    ALTER COLUMN interest_amount TYPE numeric(38,18),
    ALTER COLUMN iva_amount TYPE numeric(38,18),
    ALTER COLUMN discount_rate TYPE numeric(38,18),
    ALTER COLUMN financing_percentage TYPE numeric(38,18);

ALTER TABLE public.financing_requests
    ALTER COLUMN total_net_amount TYPE numeric(38,18),
    ALTER COLUMN total_amount_to_finance TYPE numeric(38,18);

ALTER TABLE public.disbursement_batches
    ALTER COLUMN total_amount TYPE numeric(38,18),
    ALTER COLUMN total_commission TYPE numeric(38,18),
    ALTER COLUMN total_interest TYPE numeric(38,18);
