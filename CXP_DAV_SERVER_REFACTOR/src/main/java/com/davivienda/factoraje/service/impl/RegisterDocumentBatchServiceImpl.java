package com.davivienda.factoraje.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.entities.ExcelTemplateColumnModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.SystemParameterKey;
import com.davivienda.factoraje.dto.acceptance_audit.AcceptanceAuditDTORequest;
import com.davivienda.factoraje.dto.upload_batch.BatchProcessResult;
import com.davivienda.factoraje.dto.upload_batch.InvoiceRecordDTO;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;
import com.davivienda.factoraje.infrastructure.exception.CreditLimitExceededException;
import com.davivienda.factoraje.infrastructure.exception.InactiveResourceException;
import com.davivienda.factoraje.infrastructure.exception.InvalidFileException;
import com.davivienda.factoraje.infrastructure.exception.ResourceAlreadyExistsException;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.infrastructure.file_parser.excel.BatchErrorType;
import com.davivienda.factoraje.infrastructure.file_parser.excel.BatchValidationError;
import com.davivienda.factoraje.infrastructure.file_parser.excel.ExcelParseResult;
import com.davivienda.factoraje.infrastructure.file_parser.excel.ExcelRowAccessor;
import com.davivienda.factoraje.infrastructure.file_parser.excel.GenericExcelParser;
import com.davivienda.factoraje.infrastructure.mail.MailNotice;
import com.davivienda.factoraje.infrastructure.mail.MailNoticePublisher;
import com.davivienda.factoraje.repository.EntityRepository;
import com.davivienda.factoraje.repository.ExcelTemplateColumnRepository;
import com.davivienda.factoraje.repository.TermVersionCatRepository;
import com.davivienda.factoraje.repository.UserRepository;
import com.davivienda.factoraje.service.BatchPersistenceService;
import com.davivienda.factoraje.service.DocumentValidationService;
import com.davivienda.factoraje.service.RegisterDocumentBatchService;

import static com.davivienda.factoraje.infrastructure.util.IdentifierNormalizer.upperCase;
import static com.davivienda.factoraje.infrastructure.util.IdentifierNormalizer.withoutSeparators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegisterDocumentBatchServiceImpl implements RegisterDocumentBatchService {

    private final GenericExcelParser genericExcelParser;
    private final ExcelTemplateColumnRepository excelTemplateColumnRepository;
    private final DocumentValidationService validationService;
    private final TermVersionCatRepository versionRepository;
    private final UserRepository userRepository;
    private final EntityRepository entityRepository;
    private final BatchPersistenceService batchPersistenceService;
    private final SystemParameters systemParameters;
    private final UploadBatchReports reports;
    private final MailNoticePublisher mailNotices;

    @Override
    public BatchProcessResult processFile(
            MultipartFile file,
            AcceptanceAuditDTORequest auditRequest,
            UUID payerId,
            UUID uploadedAndApprovedBy,
            boolean uploadedByAdmin) {

        EntityModel payer = requireUploadPreconditions(payerId, uploadedAndApprovedBy, auditRequest);
        String originalFilename = file.getOriginalFilename();
        UploadContext context = new UploadContext(originalFilename, payer.getName(), uploaderName(uploadedAndApprovedBy));

        try {
            return processLoadedFile(file, auditRequest, payer, uploadedAndApprovedBy, uploadedByAdmin, context);
        } catch (RuntimeException e) {
            mailNotices.uploadFailed(payer.getId(), payer.getName(), originalFilename, context.uploadedBy(),
                    e.getMessage());
            throw e;
        }
    }

    private BatchProcessResult processLoadedFile(
            MultipartFile file,
            AcceptanceAuditDTORequest auditRequest,
            EntityModel payer,
            UUID uploadedAndApprovedBy,
            boolean uploadedByAdmin,
            UploadContext context) {

        String originalFilename = context.originalFilename();
        ExcelParseResult<InvoiceRecordDTO> parseResult = parse(file, originalFilename);

        List<BatchValidationError> errors = new ArrayList<>();
        if (parseResult.hasErrors()) {
            errors.addAll(parseResult.parsingErrors());
        }

        List<InvoiceRecordDTO> parsedInvoices = parseResult.successfulRecords();
        if (parsedInvoices.isEmpty() && errors.isEmpty()) {
            throw new IllegalArgumentException("El archivo no contiene registros válidos de facturas.");
        }

        Map<String, List<InvoiceRecordDTO>> invoicesBySupplier = parsedInvoices.stream()
                .collect(Collectors.groupingBy(inv -> Objects.toString(inv.supplierNit(), "")));
        errors.addAll(validate(parsedInvoices, invoicesBySupplier));

        if (!errors.isEmpty()) {
            log.warn("La validación previa falló con {} errores. Generando PDF de rechazo. No se tocó la Base de Datos.", errors.size());
            BatchProcessResult result = new BatchProcessResult(reports.validationRejection(
                    originalFilename, context.payerName(), context.uploadedBy(), errors), false);
            mailNotices.uploadFailed(payer.getId(), payer.getName(), originalFilename, context.uploadedBy(),
                    "El archivo tiene " + errors.size() + " inconsistencias y no se guardó ningún registro.");
            return result;
        }

        return persistAndReport(payer, invoicesBySupplier, context, parsedInvoices,
                uploadedAndApprovedBy, auditRequest, uploadedByAdmin);
    }

    /** Datos del encabezado que comparten el comprobante y los reportes de rechazo. */
    private record UploadContext(String originalFilename, String payerName, String uploadedBy) {}

    private EntityModel requireUploadPreconditions(UUID payerId, UUID uploadedAndApprovedBy,
            AcceptanceAuditDTORequest auditRequest) {

        EntityModel payer = entityRepository.findById(payerId)
                .orElseThrow(() -> new ResourceNotFoundException("El pagador especificado no existe o no es válido."));
        InactiveResourceException.requireActive(payer.getStatus(),
                "El pagador " + payer.getName() + " está inactivo; no se pueden cargar documentos.");

        if (!userRepository.existsById(uploadedAndApprovedBy)) {
            throw new ResourceNotFoundException("El usuario especificado no existe.");
        }

        if (auditRequest.versionId() != null && !versionRepository.existsById(auditRequest.versionId())) {
            throw new ResourceNotFoundException("La versión de términos y condiciones especificada no existe.");
        }

        return payer;
    }

    private ExcelParseResult<InvoiceRecordDTO> parse(MultipartFile file, String originalFilename) {
        List<ExcelTemplateColumnModel> columns = excelTemplateColumnRepository.findByActiveTrue();
        List<String> requiredHeaders = columns.stream()
                .filter(ExcelTemplateColumnModel::isRequired)
                .map(ExcelTemplateColumnModel::getExcelColumnName)
                .toList();
        int maxRows = systemParameters.getInt(SystemParameterKey.UPLOAD_MAX_ROWS);

        try (InputStream inputStream = file.getInputStream()) {
            return genericExcelParser.parse(
                    inputStream,
                    requiredHeaders,
                    maxRows,
                    (accessor, rowIndex) -> toInvoiceRecord(accessor, rowIndex, columns));
        } catch (IOException e) {
            log.error("Error al leer el Excel {}", originalFilename, e);
            throw new InvalidFileException(GenericExcelParser.UNREADABLE_FILE_MESSAGE, e);
        }
    }

    private InvoiceRecordDTO toInvoiceRecord(ExcelRowAccessor accessor, int rowIndex,
            List<ExcelTemplateColumnModel> columns) {
        return new InvoiceRecordDTO(
                rowIndex,
                accessor.getLocalDate(getPhysicalColumnName(columns, "issueDate")),
                accessor.getBigDecimal(getPhysicalColumnName(columns, "nominalAmount")),
                accessor.getString(getPhysicalColumnName(columns, "documentNumber")),
                upperCase(accessor.getString(getPhysicalColumnName(columns, "generationCode"))),
                upperCase(accessor.getString(getPhysicalColumnName(columns, "receivedStamp"))),
                upperCase(accessor.getString(getPhysicalColumnName(columns, "controlNumber"))),
                accessor.getString(getPhysicalColumnName(columns, "issuanceMethod")),
                accessor.getString(getPhysicalColumnName(columns, "invoiceType")),
                withoutSeparators(accessor.getTextOnly(getPhysicalColumnName(columns, "supplierNit"))),
                accessor.getString(getPhysicalColumnName(columns, "supplierName")),
                withoutSeparators(accessor.getTextOnly(getPhysicalColumnName(columns, "supplierAccountNumber"))),
                accessor.getString(getPhysicalColumnName(columns, "paymentPolicy")),
                accessor.getString(getPhysicalColumnName(columns, "disbursementDay")));
    }

    /** Duplicados dentro del archivo y reglas de negocio por proveedor, sin tocar la base de datos. */
    private List<BatchValidationError> validate(List<InvoiceRecordDTO> parsedInvoices,
            Map<String, List<InvoiceRecordDTO>> invoicesBySupplier) {

        List<BatchValidationError> errors = new ArrayList<>();
        if (parsedInvoices.isEmpty()) {
            return errors;
        }

        errors.addAll(validationService.validateDteDuplicatesInFile(parsedInvoices));
        for (List<InvoiceRecordDTO> supplierInvoices : invoicesBySupplier.values()) {
            errors.addAll(validationService.validateBatch(supplierInvoices, null));
        }
        return errors;
    }

    private BatchProcessResult persistAndReport(
            EntityModel payer,
            Map<String, List<InvoiceRecordDTO>> invoicesBySupplier,
            UploadContext context,
            List<InvoiceRecordDTO> parsedInvoices,
            UUID uploadedAndApprovedBy,
            AcceptanceAuditDTORequest auditRequest,
            boolean uploadedByAdmin) {

        String originalFilename = context.originalFilename();
        UUID uploadBatchId;
        try {
            uploadBatchId = batchPersistenceService.persistBatch(
                    payer,
                    invoicesBySupplier,
                    originalFilename,
                    parsedInvoices,
                    uploadedAndApprovedBy,
                    auditRequest);
        } catch (CreditLimitExceededException e) {
            BatchProcessResult result = new BatchProcessResult(reports.creditLimitRejection(
                    originalFilename, context.payerName(), context.uploadedBy(), e), false);
            mailNotices.uploadFailed(payer.getId(), payer.getName(), originalFilename, context.uploadedBy(),
                    e.getMessage());
            return result;

        } catch (IllegalArgumentException e) {
            log.warn("Carga {} rechazada: {}", originalFilename, e.getMessage());
            return generalRejection(payer, context, BatchErrorType.VALUE_NOT_ALLOWED, e.getMessage());

        } catch (ResourceNotFoundException | InactiveResourceException e) {
            log.warn("Carga {} rechazada: {}", originalFilename, e.getMessage());
            return generalRejection(payer, context, BatchErrorType.RESOURCE_UNAVAILABLE, e.getMessage());

        } catch (DataIntegrityViolationException | PessimisticLockingFailureException
                | ResourceAlreadyExistsException e) {
            log.warn("Carga {} rechazada por conflicto con una operación simultánea: {}",
                    originalFilename, e.getMessage());
            return generalRejection(payer, context, BatchErrorType.CONCURRENT_UPLOAD,
                    "Otra carga registró al mismo tiempo uno o más documentos de este archivo, "
                            + "o datos de sus proveedores. "
                            + "Vuelva a cargar el archivo para ver el detalle de los registros duplicados.");

        } catch (Exception e) {
            log.error("Fallo inesperado durante la persistencia de la carga {}", originalFilename, e);
            throw new IllegalStateException("Ocurrió un error inesperado al guardar los registros en el sistema.", e);
        }

        BatchProcessResult result = new BatchProcessResult(reports.receipt(originalFilename, context.payerName(),
                context.uploadedBy(), parsedInvoices,
                registeredSupplierNames(invoicesBySupplier.keySet())), true);

        if (uploadedByAdmin) {
            mailNotices.operatorUploadSucceeded(payer.getId(), payer.getName(), uploadBatchId, originalFilename,
                    parsedInvoices.size(), context.uploadedBy());
        } else {
            mailNotices.payerUploadSucceeded(payer.getId(), payer.getName(), uploadBatchId, originalFilename,
                    parsedInvoices.size(), context.uploadedBy(), supplierDocuments(invoicesBySupplier));
        }
        return result;
    }

    private BatchProcessResult generalRejection(EntityModel payer, UploadContext context, BatchErrorType errorType,
            String message) {
        String reason = message + " No se guardó ningún registro.";
        BatchProcessResult result = new BatchProcessResult(reports.generalRejection(context.originalFilename(),
                context.payerName(), context.uploadedBy(), errorType, reason), false);
        mailNotices.uploadFailed(payer.getId(), payer.getName(), context.originalFilename(), context.uploadedBy(),
                reason);
        return result;
    }

    private List<MailNotice.SupplierDocuments> supplierDocuments(
            Map<String, List<InvoiceRecordDTO>> invoicesBySupplier) {
        List<MailNotice.SupplierDocuments> suppliers = new ArrayList<>();
        for (Map.Entry<String, List<InvoiceRecordDTO>> entry : invoicesBySupplier.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue().isEmpty()) {
                continue;
            }
            suppliers.add(new MailNotice.SupplierDocuments(
                    entry.getKey(), entry.getValue().getFirst().supplierName(), entry.getValue().size()));
        }
        return suppliers;
    }

    private String uploaderName(UUID userId) {
        return userRepository.findById(userId)
                .map(UserModel::fullName)
                .filter(name -> !name.isEmpty())
                .orElse(null);
    }

    /** Si el proveedor ya existía, la carga conserva su nombre registrado y no el del archivo. */
    private Map<String, String> registeredSupplierNames(Set<String> supplierNits) {
        Map<String, String> names = new HashMap<>();
        for (String nit : supplierNits) {
            entityRepository.findByNit(nit).ifPresent(supplier -> names.put(nit, supplier.getName()));
        }
        return names;
    }

    private String getPhysicalColumnName(List<ExcelTemplateColumnModel> columns, String logicalName) {
        return columns.stream()
                .filter(c -> c.getLogicalDtoField().equals(logicalName))
                .findFirst()
                .map(ExcelTemplateColumnModel::getExcelColumnName)
                .orElse(logicalName);
    }
}
