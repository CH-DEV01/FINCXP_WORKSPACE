-- El rol OPERATOR nunca tuvo rutas, menús ni permisos en la API: las tareas del banco
-- las realiza ADMIN, el operador bancario. Los usuarios que tenían OPERATOR pueden
-- figurar en el historial (p. ej. disbursement_batches), así que no se borran: pasan a
-- ADMIN inactivos para conservar el historial sin darles acceso.

UPDATE public.users
SET role_id = 'b1000000-0000-4000-8000-000000000001',
    status = 'INACTIVE',
    updated_at = now()
WHERE role_id = 'b1000000-0000-4000-8000-000000000006';

DELETE FROM public.role_menus WHERE role_id = 'b1000000-0000-4000-8000-000000000006';
DELETE FROM public.role_routes WHERE role_id = 'b1000000-0000-4000-8000-000000000006';
DELETE FROM public.roles_cat WHERE id = 'b1000000-0000-4000-8000-000000000006';

UPDATE public.roles_cat
SET description = 'Operador bancario',
    updated_at = now()
WHERE id = 'b1000000-0000-4000-8000-000000000001';
