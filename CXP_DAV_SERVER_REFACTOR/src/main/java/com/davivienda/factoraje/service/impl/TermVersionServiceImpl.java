package com.davivienda.factoraje.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.davivienda.factoraje.domain.catalogs.TermTypeCat;
import com.davivienda.factoraje.domain.catalogs.TermVersionCat;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;
import com.davivienda.factoraje.domain.enums.TermTypeUniqueCodeEnum;
import com.davivienda.factoraje.domain.enums.TermVersionStatusEnum;
import com.davivienda.factoraje.dto.term_version.TermTypeDTOResponse;
import com.davivienda.factoraje.dto.term_version.TermVersionAdminDTOResponse;
import com.davivienda.factoraje.dto.term_version.TermVersionDTORequest;
import com.davivienda.factoraje.dto.term_version.TermVersionDTOResponse;
import com.davivienda.factoraje.infrastructure.exception.ResourceAlreadyExistsException;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.infrastructure.exception.TermVersionConflictException;
import com.davivienda.factoraje.infrastructure.security.CurrentUserService;
import com.davivienda.factoraje.repository.AcceptanceAuditRepository;
import com.davivienda.factoraje.repository.TermTypeCatRepository;
import com.davivienda.factoraje.repository.TermVersionCatRepository;
import com.davivienda.factoraje.service.DisbursementPolicyService;
import com.davivienda.factoraje.service.TermVersionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TermVersionServiceImpl implements TermVersionService {

    private static final String OUTDATED_TERMS_MESSAGE =
            "Los términos y condiciones fueron actualizados. Revise y acepte la versión vigente.";

    private final TermVersionCatRepository termVersionRepository;
    private final TermTypeCatRepository termTypeRepository;
    private final AcceptanceAuditRepository acceptanceAuditRepository;
    private final CurrentUserService currentUserService;
    private final DisbursementPolicyService disbursementPolicyService;

    @Override
    @Transactional(readOnly = true)
    public TermVersionDTOResponse getActive(TermTypeUniqueCodeEnum termType) {
        return TermVersionDTOResponse.fromEntity(getActiveEntity(termType));
    }

    @Override
    @Transactional(readOnly = true)
    public TermVersionCat getActiveEntity(TermTypeUniqueCodeEnum termType) {
        return termVersionRepository
                .findFirstByStatusAndTermType_UniqueCodeOrderByCreatedAtDesc(
                        TermVersionStatusEnum.ACTIVE, termType.name())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay términos y condiciones vigentes para " + describe(termType) + "."));
    }

    @Override
    @Transactional(readOnly = true)
    public TermVersionCat requireActiveVersion(UUID versionId, TermTypeUniqueCodeEnum termType) {
        if (versionId == null) {
            throw new IllegalArgumentException("La versión de términos y condiciones es obligatoria.");
        }

        TermVersionCat version = termVersionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la versión de términos y condiciones con ID: " + versionId));

        if (!termType.name().equals(version.getTermType().getUniqueCode())) {
            throw new IllegalArgumentException(
                    "La versión de términos indicada no corresponde a " + describe(termType) + ".");
        }

        if (version.getStatus() != TermVersionStatusEnum.ACTIVE) {
            throw new TermVersionConflictException(OUTDATED_TERMS_MESSAGE);
        }

        return version;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TermTypeDTOResponse> getTermTypes() {
        return termTypeRepository.findByStatusOrderByTermNameAsc(GeneralStatusEnum.ACTIVE).stream()
                .map(TermTypeDTOResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TermVersionAdminDTOResponse> getVersions(String termTypeCode) {
        TermTypeCat termType = findTermType(termTypeCode);

        Map<UUID, Long> acceptances = acceptanceAuditRepository
                .countByVersionForTermType(termType.getUniqueCode()).stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));

        return termVersionRepository.findByTermType_UniqueCodeOrderByCreatedAtDesc(termType.getUniqueCode())
                .stream()
                .map(version -> TermVersionAdminDTOResponse.fromEntity(
                        version, acceptances.getOrDefault(version.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TermVersionAdminDTOResponse getVersion(UUID id) {
        return toAdminResponse(findVersion(id));
    }

    @Override
    @Transactional
    public TermVersionAdminDTOResponse createDraft(TermVersionDTORequest request) {
        TermTypeCat termType = findTermType(request.termTypeCode());
        String versionNumber = request.versionNumber().trim();

        if (termVersionRepository.existsByTermType_IdAndVersionNumberIgnoreCase(termType.getId(), versionNumber)) {
            throw new ResourceAlreadyExistsException(
                    "Ya existe la versión " + versionNumber + " para " + termType.getTermName() + ".");
        }

        TermVersionCat draft = TermVersionCat.builder()
                .termType(termType)
                .status(TermVersionStatusEnum.DRAFT)
                .build();
        applyRequest(draft, request);

        TermVersionCat saved = saveVersion(draft);
        log.info("Borrador de términos {} v{} creado con ID {}", termType.getUniqueCode(), versionNumber, saved.getId());
        return toAdminResponse(saved);
    }

    @Override
    @Transactional
    public TermVersionAdminDTOResponse updateDraft(UUID id, TermVersionDTORequest request) {
        TermVersionCat draft = findDraft(id, "editar");
        String versionNumber = request.versionNumber().trim();

        if (termVersionRepository.existsByTermType_IdAndVersionNumberIgnoreCaseAndIdNot(
                draft.getTermType().getId(), versionNumber, id)) {
            throw new ResourceAlreadyExistsException(
                    "Ya existe la versión " + versionNumber + " para " + draft.getTermType().getTermName() + ".");
        }

        applyRequest(draft, request);
        return toAdminResponse(saveVersion(draft));
    }

    @Override
    @Transactional
    public void deleteDraft(UUID id) {
        termVersionRepository.delete(findDraft(id, "eliminar"));
        log.info("Borrador de términos {} eliminado", id);
    }

    @Override
    @Transactional
    public TermVersionAdminDTOResponse publish(UUID id) {
        TermVersionCat draft = findDraft(id, "publicar");
        UserModel publisher = currentUserService.managed();

        List<TermVersionCat> previous = termVersionRepository
                .findByTermType_IdAndStatus(draft.getTermType().getId(), TermVersionStatusEnum.ACTIVE);
        previous.forEach(version -> version.setStatus(TermVersionStatusEnum.INACTIVE));

        draft.setStatus(TermVersionStatusEnum.ACTIVE);
        draft.setPublicationDate(disbursementPolicyService.businessToday());
        draft.setPublishedBy(publisher);
        draft.setContentHash(computeHash(draft));

        try {
            // El índice único parcial (un ACTIVE por tipo) exige desactivar antes de activar.
            termVersionRepository.saveAllAndFlush(previous);
            termVersionRepository.saveAndFlush(draft);
        } catch (DataIntegrityViolationException e) {
            log.error("Conflicto al publicar la versión de términos {}", id, e);
            throw new TermVersionConflictException(
                    "Otra versión se publicó al mismo tiempo. Actualice la pantalla e intente de nuevo.");
        }

        log.info("Términos {} v{} publicados por {}; versiones desactivadas: {}",
                draft.getTermType().getUniqueCode(), draft.getVersionNumber(), publisher.getDui(), previous.size());
        return toAdminResponse(draft);
    }

    /**
     * Debe producir el mismo valor que la expresión usada en db/legacy/terms_markdown_content.sql:
     * sha256(title || E'\n\n' || content || E'\n\n' || acceptance_text).
     */
    static String computeHash(TermVersionCat version) {
        String canonical = version.getTitle() + "\n\n" + version.getContent() + "\n\n" + version.getAcceptanceText();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible en la JVM", e);
        }
    }

    private void applyRequest(TermVersionCat version, TermVersionDTORequest request) {
        version.setVersionNumber(request.versionNumber().trim());
        version.setTitle(request.title().trim());
        version.setContent(normalizeLineEndings(request.content()).strip());
        version.setAcceptanceText(request.acceptanceText().trim());
        version.setDocumentUrl(request.documentUrl() == null || request.documentUrl().isBlank()
                ? null
                : request.documentUrl().trim());
    }

    private TermVersionCat saveVersion(TermVersionCat version) {
        try {
            return termVersionRepository.saveAndFlush(version);
        } catch (DataIntegrityViolationException e) {
            log.error("Violación de integridad al guardar la versión de términos", e);
            throw new ResourceAlreadyExistsException(
                    "Ya existe una versión con ese número para este tipo de término.");
        }
    }

    private TermVersionCat findVersion(UUID id) {
        return termVersionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la versión de términos con ID: " + id));
    }

    private TermVersionCat findDraft(UUID id, String action) {
        TermVersionCat version = findVersion(id);
        if (version.getStatus() != TermVersionStatusEnum.DRAFT) {
            throw new TermVersionConflictException(
                    "Solo se puede " + action + " una versión en borrador. Las versiones publicadas no se modifican; cree una versión nueva.");
        }
        return version;
    }

    private TermTypeCat findTermType(String termTypeCode) {
        if (termTypeCode == null || termTypeCode.isBlank()) {
            throw new IllegalArgumentException("El tipo de término es obligatorio.");
        }
        return termTypeRepository.findByUniqueCode(termTypeCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe el tipo de término " + termTypeCode + "."));
    }

    private TermVersionAdminDTOResponse toAdminResponse(TermVersionCat version) {
        long acceptances = version.getId() == null ? 0L : acceptanceAuditRepository.countByTermVersionId(version.getId());
        return TermVersionAdminDTOResponse.fromEntity(version, acceptances);
    }

    private static String normalizeLineEndings(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static String describe(TermTypeUniqueCodeEnum termType) {
        return termType == TermTypeUniqueCodeEnum.PAYER_TERM_TYPE ? "el pagador" : "el proveedor";
    }
}
