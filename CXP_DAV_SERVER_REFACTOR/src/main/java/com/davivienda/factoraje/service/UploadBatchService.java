package com.davivienda.factoraje.service;
import com.davivienda.factoraje.dto.upload_batch.UploadBatchDTORequest;
import com.davivienda.factoraje.dto.upload_batch.UploadBatchDTOResponse;

public interface UploadBatchService {

    public UploadBatchDTOResponse createUploadBatch(UploadBatchDTORequest request);
    
}
