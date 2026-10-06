-- El manual de la plantilla de carga deja de ser texto Markdown y pasa a ser un PDF que publica
-- el ADMIN, igual que la plantilla. El manual en Markdown que insertó V9 se elimina; hasta que se
-- publique el PDF, las pantallas de carga lo muestran como "No disponible".

DELETE FROM public.upload_resources WHERE resource_type = 'MANUAL';

ALTER TABLE public.upload_resources DROP CONSTRAINT upload_resources_content_check;
ALTER TABLE public.upload_resources DROP COLUMN content;

ALTER TABLE public.upload_resources
    ALTER COLUMN file_name SET NOT NULL,
    ALTER COLUMN file_content SET NOT NULL,
    ALTER COLUMN file_size SET NOT NULL,
    ADD CONSTRAINT upload_resources_file_size_check CHECK (file_size > 0);
