-- El pagador se registra con su NIU (solo dígitos, sin separadores). Los proveedores y los
-- pagadores registrados antes de esta versión no lo tienen; UNIQUE admite varios NULL.

ALTER TABLE public.entities ADD COLUMN niu character varying(25);
ALTER TABLE public.entities ADD CONSTRAINT uk_entities_niu UNIQUE (niu);
