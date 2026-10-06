-- Los usuarios de los proveedores se dan de alta desde la gestión de usuarios y el proveedor
-- se identifica por su NIT. El archivo de carga deja de traer el NIU y los datos del operario;
-- si las columnas vienen en el archivo, se ignoran.

UPDATE public.excel_template_columns
SET is_active = false,
    is_required = false,
    updated_at = now()
WHERE logical_dto_field IN (
    'supplierNiu',
    'operatorDui',
    'operatorEmail',
    'operatorFirstName',
    'operatorMiddleName',
    'operatorFirstLastName',
    'operatorSecondLastName');

-- Los proveedores creados desde la carga ya no tienen NIU. UNIQUE admite varios NULL.
ALTER TABLE public.entities ALTER COLUMN niu DROP NOT NULL;
