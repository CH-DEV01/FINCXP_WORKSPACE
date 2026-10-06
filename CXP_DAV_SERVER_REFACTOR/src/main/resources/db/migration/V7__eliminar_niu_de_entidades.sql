-- Las entidades se identifican por su NIT; el NIU deja de existir en el modelo.
-- Su restricción UNIQUE se elimina junto con la columna.

ALTER TABLE public.entities DROP COLUMN niu;
