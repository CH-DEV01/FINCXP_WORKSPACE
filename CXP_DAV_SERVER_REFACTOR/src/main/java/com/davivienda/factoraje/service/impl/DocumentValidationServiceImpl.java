package com.davivienda.factoraje.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.davivienda.factoraje.domain.catalogs.DisbursementPolicyCat;
import com.davivienda.factoraje.domain.catalogs.PaymentPolicyCat;
import com.davivienda.factoraje.domain.constants.EntityTypeCode;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.enums.InvoiceTypeEnum;
import com.davivienda.factoraje.domain.enums.IssuanceMethodEnum;
import com.davivienda.factoraje.domain.enums.SystemParameterKey;
import com.davivienda.factoraje.dto.upload_batch.InvoiceRecordDTO;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;
import com.davivienda.factoraje.infrastructure.file_parser.excel.BatchValidationError;
import com.davivienda.factoraje.infrastructure.util.Money;
import com.davivienda.factoraje.repository.BankAccountRepository;
import com.davivienda.factoraje.repository.DisbursementPolicyCatRepository;
import com.davivienda.factoraje.repository.DocumentRepository;
import com.davivienda.factoraje.repository.EntityRepository;
import com.davivienda.factoraje.repository.MasterAgreementRepository;
import com.davivienda.factoraje.repository.PaymentPolicyCatRepository;
import com.davivienda.factoraje.service.DisbursementPolicyService;
import com.davivienda.factoraje.service.DocumentValidationService;

import static com.davivienda.factoraje.infrastructure.file_parser.excel.BatchErrorType.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentValidationServiceImpl implements DocumentValidationService {

    private final DocumentRepository documentRepository;
    private final MasterAgreementRepository masterAgreementRepository;
    private final EntityRepository entityRepository;
    private final BankAccountRepository bankAccountRepository;
    private final PaymentPolicyCatRepository paymentPolicyCatRepository;
    private final DisbursementPolicyCatRepository disbursementPolicyCatRepository;
    private final SystemParameters systemParameters;
    private final DisbursementPolicyService disbursementPolicyService;

    // Formatos del esquema de DTE del Ministerio de Hacienda. Los valores llegan ya en mayúsculas.
    private static final Pattern GENERATION_CODE_PATTERN = Pattern
            .compile("^[A-F0-9]{8}-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{12}$");
    private static final Pattern CONTROL_NUMBER_PATTERN = Pattern.compile("^DTE-(\\d{2})-[A-Z0-9]{8}-\\d{15}$");
    private static final Pattern RECEIVED_STAMP_PATTERN = Pattern.compile("^[A-Z0-9]{40}$");

    // Identificadores ya normalizados sin guiones ni espacios.
    private static final Pattern NIT_PATTERN = Pattern.compile("^\\d{14}$");
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^\\d+$");
    private static final int MAX_ACCOUNT_LENGTH = 50;
    private static final String BANK_ACCOUNT_COLUMN = "Cuenta bancaria";

    @Override
    public List<BatchValidationError> validateBatch(List<InvoiceRecordDTO> invoices, UUID masterAgreementId) {

        List<BatchValidationError> errors = new ArrayList<>();
        LocalDate today = disbursementPolicyService.businessToday();
        int maxInvoiceAgeDays = systemParameters.getInt(SystemParameterKey.MAX_INVOICE_AGE_DAYS);

        // Validaciones masivas contra base de datos (doble fondeo)
        validateDoubleFundingPaperBulk(invoices, errors);
        validateDoubleFundingDteBulk(invoices, InvoiceRecordDTO::generationCode,
                documentRepository::findExistingGenerationCodes, "Codigo de generacion", "código de generación", errors);
        validateDoubleFundingDteBulk(invoices, InvoiceRecordDTO::controlNumber,
                documentRepository::findExistingControlNumbers, "Numero de control", "número de control", errors);
        validateDoubleFundingDteBulk(invoices, InvoiceRecordDTO::receivedStamp,
                documentRepository::findExistingReceivedStamps, "Sello de recepcion", "sello de recepción", errors);
        Optional<EntityModel> existingSupplier = findExistingSupplier(invoices, errors);
        validateSupplierBankAccount(invoices, existingSupplier, errors);

        Set<String> paymentPolicyCodes = paymentPolicyCatRepository.findAll().stream()
                .map(PaymentPolicyCat::getCode)
                .collect(Collectors.toSet());
        Set<String> disbursementPolicyCodes = disbursementPolicyCatRepository.findAll().stream()
                .map(DisbursementPolicyCat::getCode)
                .collect(Collectors.toSet());

        Set<String> seenPhysicalKeys = new HashSet<>();

        // Validaciones individuales por registro
        for (InvoiceRecordDTO invoice : invoices) {

            IssuanceMethodEnum method = IssuanceMethodEnum.fromString(invoice.issuanceMethod());
            validateRequiredText(invoice.issuanceMethod(), "Forma de emision", 30, invoice.rowIndex(), errors);
            if (method == null && invoice.issuanceMethod() != null && !invoice.issuanceMethod().isBlank()) {
                errors.add(new BatchValidationError(invoice.rowIndex(), "Forma de emision", VALUE_NOT_ALLOWED,
                        "Valor no permitido. Debe ser DIGITAL o PAPER. Valor recibido: " + invoice.issuanceMethod()));
            }

            if (method == IssuanceMethodEnum.PAPER) {
                if (invoice.documentNumber() == null || invoice.documentNumber().isBlank()) {
                    errors.add(new BatchValidationError(invoice.rowIndex(), "Numero de documento", REQUIRED_FIELD,
                            "El número de documento es obligatorio para facturas físicas (PAPER)."));
                } else if (invoice.documentNumber().length() > 255) {
                    errors.add(new BatchValidationError(invoice.rowIndex(), "Numero de documento", MAX_LENGTH_EXCEEDED,
                            "El número de documento físico supera el máximo de 255 caracteres."));
                }
            }

            validateRequiredText(invoice.invoiceType(), "Tipo de documento", 30, invoice.rowIndex(), errors);

            // Validaciones para Creación de Entidades (Proveedor)
            validateRequiredPattern(invoice.supplierNit(), "NIT del proveedor", NIT_PATTERN,
                    "El NIT debe tener exactamente 14 dígitos.", invoice.rowIndex(), errors);
            validateRequiredText(invoice.supplierName(), "Nombre del proveedor", 255, invoice.rowIndex(), errors);
            validateCatalogCode(invoice.paymentPolicy(), "Politica de pago", paymentPolicyCodes,
                    "La política de pago no existe en el catálogo.", invoice.rowIndex(), errors);
            validateCatalogCode(invoice.disbursementDay(), "Dia de desembolso", disbursementPolicyCodes,
                    "El día de desembolso no existe en el catálogo de políticas de desembolso.", invoice.rowIndex(), errors);

            validateDteFormat(invoice, errors);

            if (invoice.nominalAmount() == null) {
                errors.add(new BatchValidationError(invoice.rowIndex(), "Monto", REQUIRED_FIELD,
                        "El monto nominal de la factura es obligatorio y no puede estar vacío."));
            } else if (invoice.nominalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add(new BatchValidationError(invoice.rowIndex(), "Monto", VALUE_NOT_ALLOWED,
                        "El monto nominal de la factura debe ser mayor a cero."));
            } else if (Money.hasMoreThanTwoDecimals(invoice.nominalAmount())) {
                errors.add(new BatchValidationError(invoice.rowIndex(), "Monto", INVALID_FORMAT,
                        "El monto nominal admite como máximo 2 decimales. Valor recibido: "
                                + invoice.nominalAmount().stripTrailingZeros().toPlainString()));
            }

            if (invoice.issueDate() != null) {
                if (invoice.issueDate().isAfter(today)) {
                    errors.add(new BatchValidationError(invoice.rowIndex(), "Fecha de emision", INVALID_DATE,
                            "La fecha de emisión no puede ser futura."));
                } else if (invoice.issueDate().isBefore(today.minusDays(maxInvoiceAgeDays))) {
                    errors.add(new BatchValidationError(invoice.rowIndex(), "Fecha de emision", INVALID_DATE,
                            "La factura supera la antigüedad máxima permitida de " + maxInvoiceAgeDays + " días."));
                }
            } else {
                errors.add(new BatchValidationError(invoice.rowIndex(), "Fecha de emision", REQUIRED_FIELD,
                        "La fecha de emisión es obligatoria."));
            }

            if (method == IssuanceMethodEnum.PAPER && invoice.supplierNit() != null
                    && invoice.documentNumber() != null) {
                String physicalKey = invoice.supplierNit() + "-" + invoice.documentNumber();
                if (!seenPhysicalKeys.add(physicalKey)) {
                    errors.add(new BatchValidationError(invoice.rowIndex(), "Numero de documento", DUPLICATE_IN_FILE,
                            "Esta factura física viene duplicada dentro de este mismo archivo Excel para este proveedor."));
                }
            }
        }

        return errors;
    }

    /**
     * Los DTE son únicos en todo el país, así que el duplicado se busca en el archivo completo
     * y no por proveedor como en {@link #validateBatch}.
     */
    @Override
    public List<BatchValidationError> validateDteDuplicatesInFile(List<InvoiceRecordDTO> invoices) {
        List<BatchValidationError> errors = new ArrayList<>();
        Set<String> seenGenerationCodes = new HashSet<>();
        Set<String> seenControlNumbers = new HashSet<>();
        Set<String> seenReceivedStamps = new HashSet<>();

        for (InvoiceRecordDTO invoice : invoices) {
            if (IssuanceMethodEnum.fromString(invoice.issuanceMethod()) != IssuanceMethodEnum.DIGITAL) continue;

            if (invoice.generationCode() != null && !seenGenerationCodes.add(invoice.generationCode())) {
                errors.add(new BatchValidationError(invoice.rowIndex(), "Codigo de generacion", DUPLICATE_IN_FILE,
                        "Este código de generación viene duplicado dentro de este mismo archivo Excel."));
            }
            if (invoice.controlNumber() != null && !seenControlNumbers.add(invoice.controlNumber())) {
                errors.add(new BatchValidationError(invoice.rowIndex(), "Numero de control", DUPLICATE_IN_FILE,
                        "Este número de control viene duplicado dentro de este mismo archivo Excel."));
            }
            if (invoice.receivedStamp() != null && !seenReceivedStamps.add(invoice.receivedStamp())) {
                errors.add(new BatchValidationError(invoice.rowIndex(), "Sello de recepcion", DUPLICATE_IN_FILE,
                        "Este sello de recepción viene duplicado dentro de este mismo archivo Excel."));
            }
        }
        return errors;
    }

    private void validateRequiredText(String value, String columnName, int maxLength, int rowIndex,
            List<BatchValidationError> errors) {
        if (value == null || value.isBlank()) {
            errors.add(new BatchValidationError(rowIndex, columnName, REQUIRED_FIELD,
                    "El campo es obligatorio y no puede estar vacío."));
        } else if (value.length() > maxLength) {
            errors.add(new BatchValidationError(rowIndex, columnName, MAX_LENGTH_EXCEEDED,
                    String.format("El campo supera la longitud máxima permitida de %d caracteres. Valor recibido: %d caracteres.",
                            maxLength, value.length())));
        }
    }

    private void validateCatalogCode(String value, String columnName, Set<String> validCodes, String message,
            int rowIndex, List<BatchValidationError> errors) {
        if (value == null || value.isBlank()) {
            errors.add(new BatchValidationError(rowIndex, columnName, REQUIRED_FIELD,
                    "El campo es obligatorio y no puede estar vacío."));
        } else if (!validCodes.contains(value)) {
            errors.add(new BatchValidationError(rowIndex, columnName, VALUE_NOT_ALLOWED,
                    message + " Valor recibido: " + value));
        }
    }

    private void validateRequiredPattern(String value, String columnName, Pattern pattern, String message,
            int rowIndex, List<BatchValidationError> errors) {
        if (value == null || value.isBlank()) {
            errors.add(new BatchValidationError(rowIndex, columnName, REQUIRED_FIELD,
                    "El campo es obligatorio y no puede estar vacío."));
        } else if (!pattern.matcher(value).matches()) {
            errors.add(new BatchValidationError(rowIndex, columnName, INVALID_FORMAT,
                    message + " Valor recibido: " + value));
        }
    }

    private void validateDteFormat(InvoiceRecordDTO dto, List<BatchValidationError> errors) {

        InvoiceTypeEnum tipoDocEnum = InvoiceTypeEnum.fromString(dto.invoiceType());
        if (tipoDocEnum == null && dto.invoiceType() != null && !dto.invoiceType().isBlank()) {
            errors.add(new BatchValidationError(dto.rowIndex(), "Tipo de documento", VALUE_NOT_ALLOWED,
                    "Valor no permitido. Debe ser CCF (Comprobante de Crédito Fiscal) o FCI (Factura de consumidor final). Valor recibido: "
                            + dto.invoiceType()));
        }

        IssuanceMethodEnum formaEmisionEnum = IssuanceMethodEnum.fromString(dto.issuanceMethod());
        if (formaEmisionEnum == null) return;

        if (formaEmisionEnum == IssuanceMethodEnum.DIGITAL) {
            validateRequiredPattern(dto.generationCode(), "Codigo de generacion", GENERATION_CODE_PATTERN,
                    "El código de generación debe ser un UUID de 36 caracteres (8-4-4-4-12, hexadecimal).",
                    dto.rowIndex(), errors);
            validateRequiredPattern(dto.receivedStamp(), "Sello de recepcion", RECEIVED_STAMP_PATTERN,
                    "El sello de recepción debe tener exactamente 40 caracteres, solo letras y números.",
                    dto.rowIndex(), errors);
            validateControlNumber(dto, tipoDocEnum, errors);
        } else if (formaEmisionEnum == IssuanceMethodEnum.PAPER) {
            if (dto.generationCode() != null && !dto.generationCode().isBlank()) {
                errors.add(new BatchValidationError(dto.rowIndex(), "Codigo de generacion", VALUE_NOT_ALLOWED,
                        "Los documentos en papel (PAPER) no deben contener código de generación."));
            }
            if (dto.receivedStamp() != null && !dto.receivedStamp().isBlank()) {
                errors.add(new BatchValidationError(dto.rowIndex(), "Sello de recepcion", VALUE_NOT_ALLOWED,
                        "Los documentos en papel (PAPER) no deben contener sello de recepción."));
            }
            if (dto.controlNumber() != null && !dto.controlNumber().isBlank()) {
                errors.add(new BatchValidationError(dto.rowIndex(), "Numero de control", VALUE_NOT_ALLOWED,
                        "Los documentos en papel (PAPER) no deben contener número de control DTE."));
            }
        }
    }

    private void validateControlNumber(InvoiceRecordDTO dto, InvoiceTypeEnum invoiceType, List<BatchValidationError> errors) {
        String controlNumber = dto.controlNumber();
        if (controlNumber == null || controlNumber.isBlank()) {
            errors.add(new BatchValidationError(dto.rowIndex(), "Numero de control", REQUIRED_FIELD,
                    "El campo es obligatorio y no puede estar vacío."));
            return;
        }

        Matcher matcher = CONTROL_NUMBER_PATTERN.matcher(controlNumber);
        if (!matcher.matches()) {
            errors.add(new BatchValidationError(dto.rowIndex(), "Numero de control", INVALID_FORMAT,
                    "El número de control debe tener el formato DTE-TT-XXXXXXXX-NNNNNNNNNNNNNNN (tipo de DTE, establecimiento y punto de venta, correlativo de 15 dígitos). Valor recibido: "
                            + controlNumber));
            return;
        }

        String dteCode = matcher.group(1);
        InvoiceTypeEnum typeInControlNumber = InvoiceTypeEnum.fromDteCode(dteCode);
        if (typeInControlNumber == null) {
            errors.add(new BatchValidationError(dto.rowIndex(), "Numero de control", VALUE_NOT_ALLOWED,
                    String.format("El tipo de DTE %s no es financiable. Solo se aceptan 01 (Factura de consumidor final) y 03 (Comprobante de Crédito Fiscal).",
                            dteCode)));
        } else if (invoiceType != null && typeInControlNumber != invoiceType) {
            errors.add(new BatchValidationError(dto.rowIndex(), "Numero de control", VALUE_NOT_ALLOWED,
                    String.format("El número de control corresponde al tipo %s (%s), pero el tipo de documento indicado es %s (%s).",
                            dteCode, typeInControlNumber.getDescription(), invoiceType.name(), invoiceType.getDteCode())));
        }
    }

    /**
     * El NIT del archivo identifica al proveedor. Si ya está registrado para un pagador o un banco,
     * la carga se rechaza en vez de asociarle documentos como si fuera proveedor.
     */
    private Optional<EntityModel> findExistingSupplier(List<InvoiceRecordDTO> invoices,
            List<BatchValidationError> errors) {

        if (invoices.isEmpty() || invoices.get(0).supplierNit() == null) return Optional.empty();

        InvoiceRecordDTO firstInvoice = invoices.get(0);
        Optional<EntityModel> existing = entityRepository.findByNit(firstInvoice.supplierNit());
        if (existing.isPresent() && !EntityTypeCode.SUPPLIER.equals(existing.get().getEntityType().getCode())) {
            errors.add(new BatchValidationError(firstInvoice.rowIndex(), "NIT del proveedor", SUPPLIER_ACCOUNT,
                    String.format("El NIT %s ya está registrado para %s, que no es un proveedor.",
                            firstInvoice.supplierNit(), existing.get().getName())));
            return Optional.empty();
        }
        return existing;
    }

    /**
     * Se invoca con las facturas de un solo proveedor. Todas deben traer la misma cuenta,
     * la cuenta no puede pertenecer a otra entidad y, si el proveedor ya existe con una
     * cuenta principal, debe coincidir (el cambio se hace desde la gestión de proveedores).
     */
    private void validateSupplierBankAccount(List<InvoiceRecordDTO> invoices, Optional<EntityModel> existingSupplier,
            List<BatchValidationError> errors) {

        String expectedAccount = null;

        for (InvoiceRecordDTO invoice : invoices) {
            String account = invoice.supplierAccountNumber() != null ? invoice.supplierAccountNumber().trim() : null;

            if (account == null || account.isEmpty()) {
                errors.add(new BatchValidationError(invoice.rowIndex(), BANK_ACCOUNT_COLUMN, REQUIRED_FIELD,
                        "El campo es obligatorio y no puede estar vacío."));
                continue;
            }
            if (account.length() > MAX_ACCOUNT_LENGTH || !ACCOUNT_NUMBER_PATTERN.matcher(account).matches()) {
                errors.add(new BatchValidationError(invoice.rowIndex(), BANK_ACCOUNT_COLUMN, INVALID_FORMAT,
                        String.format("La cuenta bancaria solo puede contener números (máximo %d). Valor recibido: %s",
                                MAX_ACCOUNT_LENGTH, account)));
                continue;
            }

            if (expectedAccount == null) {
                expectedAccount = account;
            } else if (!expectedAccount.equals(account)) {
                errors.add(new BatchValidationError(invoice.rowIndex(), BANK_ACCOUNT_COLUMN, SUPPLIER_ACCOUNT,
                        String.format("Todas las facturas del proveedor (NIT: %s) deben tener la misma cuenta bancaria. Se esperaba %s y se recibió %s.",
                                invoice.supplierNit(), expectedAccount, account)));
            }
        }

        if (expectedAccount == null || invoices.isEmpty()) return;

        InvoiceRecordDTO firstInvoice = invoices.get(0);
        String supplierNit = firstInvoice.supplierNit();

        final String accountNumber = expectedAccount;
        bankAccountRepository.findAllByAccountNumberIn(List.of(accountNumber)).stream()
                .filter(account -> existingSupplier
                        .map(supplier -> !supplier.getId().equals(account.getEntityModel().getId()))
                        .orElse(true))
                .findFirst()
                .ifPresent(account -> errors.add(new BatchValidationError(firstInvoice.rowIndex(), BANK_ACCOUNT_COLUMN,
                        SUPPLIER_ACCOUNT,
                        String.format("La cuenta bancaria %s ya está registrada para otra entidad.", accountNumber))));

        existingSupplier
                .flatMap(supplier -> bankAccountRepository.findFirstByEntityModelIdAndIsMainTrue(supplier.getId()))
                .filter(mainAccount -> !mainAccount.getAccountNumber().equals(accountNumber))
                .ifPresent(mainAccount -> errors.add(new BatchValidationError(firstInvoice.rowIndex(), BANK_ACCOUNT_COLUMN,
                        SUPPLIER_ACCOUNT,
                        String.format("El proveedor (NIT: %s) ya tiene registrada la cuenta %s y el archivo indica %s. Actualice la cuenta desde la gestión de proveedores antes de cargar.",
                                supplierNit, mainAccount.getAccountNumber(), accountNumber))));
    }

    /**
     * Incluye documentos en cualquier estado: una factura inactivada tampoco puede volver a cargarse.
     */
    private void validateDoubleFundingDteBulk(List<InvoiceRecordDTO> invoices,
            Function<InvoiceRecordDTO, String> field,
            Function<List<String>, Set<String>> existingLookup,
            String columnName, String fieldLabel, List<BatchValidationError> errors) {

        List<InvoiceRecordDTO> digitalInvoices = invoices.stream()
                .filter(inv -> IssuanceMethodEnum.fromString(inv.issuanceMethod()) == IssuanceMethodEnum.DIGITAL)
                .filter(inv -> field.apply(inv) != null && !field.apply(inv).isBlank())
                .toList();

        if (digitalInvoices.isEmpty()) return;
        Set<String> existingValues = existingLookup.apply(digitalInvoices.stream().map(field).distinct().toList());

        for (InvoiceRecordDTO invoice : digitalInvoices) {
            if (existingValues.contains(field.apply(invoice))) {
                errors.add(new BatchValidationError(invoice.rowIndex(), columnName, DOUBLE_FUNDING,
                        String.format("Riesgo de Doble Fondeo: ya existe un documento electrónico (DTE) registrado con este %s.", fieldLabel)));
            }
        }
    }

    private void validateDoubleFundingPaperBulk(List<InvoiceRecordDTO> invoices, List<BatchValidationError> errors) {
        List<InvoiceRecordDTO> paperInvoices = invoices.stream()
                .filter(inv -> IssuanceMethodEnum.fromString(inv.issuanceMethod()) == IssuanceMethodEnum.PAPER)
                .filter(inv -> inv.supplierNit() != null && inv.issueDate() != null && inv.documentNumber() != null)
                .toList();

        if (paperInvoices.isEmpty()) return;

        var groupedInvoices = paperInvoices.stream()
                .collect(Collectors.groupingBy(
                        InvoiceRecordDTO::supplierNit,
                        Collectors.groupingBy(inv -> inv.issueDate().getYear())));

        groupedInvoices.forEach((nit, yearMap) -> {
            yearMap.forEach((year, invoicesInGroup) -> {
                List<String> documentNumbers = invoicesInGroup.stream()
                        .map(InvoiceRecordDTO::documentNumber)
                        .toList();

                Set<String> existingNumbers = documentRepository.findExistingPhysicalDocuments(nit, documentNumbers, year);

                for (InvoiceRecordDTO invoice : invoicesInGroup) {
                    if (existingNumbers.contains(invoice.documentNumber())) {
                        errors.add(new BatchValidationError(invoice.rowIndex(), "Numero de documento", DOUBLE_FUNDING,
                                String.format("Riesgo de Doble Fondeo: El documento físico '%s' del proveedor (NIT: %s) para el año %d ya se encuentra registrado en el sistema.",
                                        invoice.documentNumber(), nit, year)));
                    }
                }
            });
        });
    }
}