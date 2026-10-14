-- Sesiones que deja el handoff del SSO para que el portal las active una sola vez
-- (vencen a los 5 minutos).

CREATE TABLE public.sso_pending_sessions (
    session_token character varying(255) NOT NULL,
    user_email character varying(255) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT sso_pending_sessions_pkey PRIMARY KEY (session_token)
);
