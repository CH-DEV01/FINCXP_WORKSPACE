package com.davivienda.factoraje.service.impl;

import org.springframework.stereotype.Service;

import com.davivienda.factoraje.domain.catalogs.TermVersionCat;
import com.davivienda.factoraje.domain.entities.AcceptanceAuditModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.TermVersionStatusEnum;
import com.davivienda.factoraje.dto.acceptance_audit.AcceptanceAuditDTORequest;
import com.davivienda.factoraje.dto.acceptance_audit.AcceptanceAuditDTOResponse;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.infrastructure.exception.TermVersionConflictException;
import com.davivienda.factoraje.repository.AcceptanceAuditRepository;
import com.davivienda.factoraje.repository.TermVersionCatRepository;
import com.davivienda.factoraje.repository.UserRepository;
import com.davivienda.factoraje.service.AcceptanceAuditService;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AcceptanceAuditServiceImpl implements AcceptanceAuditService {

    private final AcceptanceAuditRepository acceptanceAuditRepository;
    private final UserRepository userRepository;
    private final TermVersionCatRepository termVersionCatRepository;


    @Override
    @Transactional
    public AcceptanceAuditDTOResponse createAcceptanceAudit(AcceptanceAuditDTORequest request) {

        log.info("Starting the creation of a new acceptance audit");

        UserModel user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario con ID: " + request.userId()));

        TermVersionCat termVersion = termVersionCatRepository.findById(request.versionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la versión de términos con ID: " + request.versionId()));

        if (termVersion.getStatus() != TermVersionStatusEnum.ACTIVE) {
            throw new TermVersionConflictException(
                    "Los términos y condiciones fueron actualizados. Revise y acepte la versión vigente.");
        }

        AcceptanceAuditModel acceptanceAuditModel = AcceptanceAuditModel.builder()
                .userAgent(request.userAgent())
                .user(user)
                .termVersion(termVersion)
                .build();

        AcceptanceAuditModel savedAcceptanceAudit = acceptanceAuditRepository.save(acceptanceAuditModel);
        log.info("Acceptance audit created successfully with ID: {}", savedAcceptanceAudit.getId());
        return AcceptanceAuditDTOResponse.fromEntity(savedAcceptanceAudit);

    }
}
