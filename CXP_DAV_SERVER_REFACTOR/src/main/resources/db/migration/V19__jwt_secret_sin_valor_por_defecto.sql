-- JWT_SECRET deja de tener una llave por defecto: la que colocó V15 está en el código
-- fuente. Vuelve a CONFIGURAR y la API no arranca hasta que el ambiente coloque su
-- propia llave. Si el ambiente ya guardó otra llave, no se toca.

UPDATE public.system_parameters
SET param_value = 'CONFIGURAR',
    updated_at = now()
WHERE param_key = 'JWT_SECRET'
  AND param_value = 'G9UuPSmEz8a18PARiE8Rs9QKvDrdUOJjjNe3duoQqUw=';
