package com.davivienda.factoraje.dto.upload_batch;

import java.util.UUID;

import com.davivienda.factoraje.domain.enums.UploadBatchStatusEnum;

public record UploadBatchDTORequest(

    String batchNumber,
    String fileName,
    Integer totalRecords,
    UploadBatchStatusEnum status,
    UUID uploadedAndApprovedBy

){

}
