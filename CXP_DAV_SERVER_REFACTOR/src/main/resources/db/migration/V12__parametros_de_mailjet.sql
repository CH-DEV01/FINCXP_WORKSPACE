-- Parámetros de Mailjet y la URL del botón de acceso. El valor CONFIGURAR indica que el
-- ambiente todavía no tiene credenciales; el envío no llama a Mailjet hasta reemplazarlo.

INSERT INTO public.system_parameters (id, created_at, param_key, updated_at, param_value) VALUES
    ('d1200000-0000-4000-8000-000000000001', now(), 'MAILJET_API_KEY', now(), 'CONFIGURAR'),
    ('d1200000-0000-4000-8000-000000000002', now(), 'MAILJET_API_SECRET', now(), 'CONFIGURAR'),
    ('d1200000-0000-4000-8000-000000000003', now(), 'MAILJET_FROM_EMAIL', now(), 'CONFIGURAR'),
    ('d1200000-0000-4000-8000-000000000004', now(), 'MAILJET_FROM_NAME', now(), 'CONFIGURAR'),
    ('d1200000-0000-4000-8000-000000000005', now(), 'APP_LOGIN_URL', now(), 'CONFIGURAR');
