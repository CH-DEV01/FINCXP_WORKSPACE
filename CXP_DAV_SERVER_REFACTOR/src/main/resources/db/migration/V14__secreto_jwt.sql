-- Llave HMAC de los JWT. El valor inicial no firma tokens: hay que reemplazarlo
-- por una llave Base64 de al menos 32 bytes (openssl rand -base64 32) antes de
-- arrancar la API. En un ambiente que ya firmaba con la variable JWT_SECRET,
-- copiar ese mismo valor para no invalidar las sesiones emitidas.

INSERT INTO public.system_parameters (id, created_at, param_key, updated_at, param_value) VALUES
    ('d1400000-0000-4000-8000-000000000001', now(), 'JWT_SECRET', now(), 'CONFIGURAR');
