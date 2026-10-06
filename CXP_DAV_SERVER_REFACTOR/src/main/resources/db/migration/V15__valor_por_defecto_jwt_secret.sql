-- Reemplaza el marcador CONFIGURAR de JWT_SECRET por la llave por defecto.
-- Si el ambiente ya guardó otra llave, no se toca.

UPDATE public.system_parameters
SET param_value = 'G9UuPSmEz8a18PARiE8Rs9QKvDrdUOJjjNe3duoQqUw=',
    updated_at = now()
WHERE param_key = 'JWT_SECRET'
  AND param_value = 'CONFIGURAR';
