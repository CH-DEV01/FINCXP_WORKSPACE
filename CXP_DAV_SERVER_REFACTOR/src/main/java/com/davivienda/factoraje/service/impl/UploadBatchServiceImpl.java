package com.davivienda.factoraje.service.impl;

import org.springframework.stereotype.Service;
import com.davivienda.factoraje.domain.entities.UploadBatchModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.dto.upload_batch.UploadBatchDTORequest;
import com.davivienda.factoraje.dto.upload_batch.UploadBatchDTOResponse;
import com.davivienda.factoraje.infrastructure.exception.ResourceAlreadyExistsException;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.repository.UploadBatchRepository;
import com.davivienda.factoraje.repository.UserRepository;
import com.davivienda.factoraje.service.UploadBatchService;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadBatchServiceImpl implements UploadBatchService {

    private final UploadBatchRepository uploadBatchRepository;
    private final UserRepository userRepository;


    private void validateUniqueness(UploadBatchDTORequest request) {

        log.info("Validating uniqueness for batchNumber: {}", request.batchNumber());
        
        boolean exists = uploadBatchRepository.existsByBatchNumber(request.batchNumber());

        if (exists) {
            log.warn("Business rule violation: Attempt to duplicate upload batch");
            throw new ResourceAlreadyExistsException("El lote de carga ya existe.");
        }
    }

    @Transactional
    @Override
    public UploadBatchDTOResponse createUploadBatch(UploadBatchDTORequest request) {

        log.info("Initiating the creation of a new upload batch");

        validateUniqueness(request);

        UserModel uploadedAndApprovedBy = userRepository.findById(request.uploadedAndApprovedBy())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario con ID: " + request.uploadedAndApprovedBy()));

        UploadBatchModel uploadBatch = UploadBatchModel.builder()
                .batchNumber(request.batchNumber())
                .fileName(request.fileName())
                .totalRecords(request.totalRecords())
                .status(request.status())
                .uploadedAndApprovedBy(uploadedAndApprovedBy)
                .build();

        UploadBatchModel savedUploadBatch = uploadBatchRepository.save(uploadBatch);
        log.info("Upload batch created successfully with ID: {}", savedUploadBatch.getId());
        return UploadBatchDTOResponse.fromEntity(savedUploadBatch);

    }

}
