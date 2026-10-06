package com.davivienda.factoraje.service;

import org.springframework.web.multipart.MultipartFile;

import com.davivienda.factoraje.dto.upload_resource.ResourceFile;
import com.davivienda.factoraje.dto.upload_resource.UploadResourcesDTOResponse;

public interface UploadResourceService {

    UploadResourcesDTOResponse getResources();

    ResourceFile getTemplate();

    ResourceFile getManual();

    /** Valida que sea un .xlsx sin macros con exactamente las columnas activas de la carga. */
    UploadResourcesDTOResponse.ResourceInfo replaceTemplate(MultipartFile file);

    /** Valida que sea un PDF sin contraseña, JavaScript, acciones de ejecución ni archivos incrustados. */
    UploadResourcesDTOResponse.ResourceInfo replaceManual(MultipartFile file);
}
