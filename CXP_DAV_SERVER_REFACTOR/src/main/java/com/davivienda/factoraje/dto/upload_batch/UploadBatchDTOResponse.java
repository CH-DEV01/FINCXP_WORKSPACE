package com.davivienda.factoraje.dto.upload_batch;

import java.util.UUID;

import com.davivienda.factoraje.domain.entities.UploadBatchModel;
import com.davivienda.factoraje.domain.enums.UploadBatchStatusEnum;

public record UploadBatchDTOResponse(

        UUID id,
        String batchNumber,
        String fileName,
        Integer totalRecords,
        UploadBatchStatusEnum status,
        UUID uploadedAndApprovedBy

) {

    public static UploadBatchDTOResponse fromEntity(UploadBatchModel model) {
        return new UploadBatchDTOResponse(
                model.getId(),
                model.getBatchNumber(),
                model.getFileName(),
                model.getTotalRecords(),
                model.getStatus(),
                model.getUploadedAndApprovedBy().getId());
    }

}