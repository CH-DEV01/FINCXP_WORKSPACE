-- Al cerrar sesión se guarda el momento en el usuario y la API rechaza los JWT
-- emitidos antes, aunque no hayan vencido.

ALTER TABLE public.users ADD COLUMN sessions_revoked_at timestamp(6) with time zone;
