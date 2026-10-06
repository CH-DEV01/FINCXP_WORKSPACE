-- Endpoint de Mailjet. El cliente lo usa tal cual; el valor inicial es el que arma
-- mailjet-client 4.2.0.

INSERT INTO public.system_parameters (id, created_at, param_key, updated_at, param_value) VALUES
    ('d1300000-0000-4000-8000-000000000001', now(), 'MAILJET_API_URL', now(), 'https://api.mailjet.com/v3/send');
