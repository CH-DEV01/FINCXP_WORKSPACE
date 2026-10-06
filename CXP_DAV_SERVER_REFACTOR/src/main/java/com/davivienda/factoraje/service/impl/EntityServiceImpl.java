package com.davivienda.factoraje.service.impl;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.davivienda.factoraje.domain.catalogs.EntityTypeCat;
import com.davivienda.factoraje.domain.catalogs.RoleCat;
import com.davivienda.factoraje.domain.constants.EntityTypeCode;
import com.davivienda.factoraje.domain.entities.BankAccountModel;
import com.davivienda.factoraje.domain.entities.CreditFacilityHistoryModel;
import com.davivienda.factoraje.domain.entities.CreditFacilityModel;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.entities.ProductPricingTermModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;
import com.davivienda.factoraje.domain.enums.RepaymentTypeEnum;
import com.davivienda.factoraje.dto.entity.EntityDTORequest;
import com.davivienda.factoraje.dto.entity.EntityDTOResponse;
import com.davivienda.factoraje.dto.entity.PayerRegistrationDTORequest;
import com.davivienda.factoraje.dto.entity.PayerSummaryDTOResponse;
import com.davivienda.factoraje.dto.entity.PayerUpdateDTORequest;
import com.davivienda.factoraje.dto.entity.SupplierSummaryDTOResponse;
import com.davivienda.factoraje.dto.entity.SupplierUpdateDTORequest;
import com.davivienda.factoraje.infrastructure.exception.ResourceAlreadyExistsException;
import com.davivienda.factoraje.infrastructure.mail.AuditText;
import com.davivienda.factoraje.infrastructure.mail.MailNoticePublisher;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.infrastructure.security.CurrentUserService;
import com.davivienda.factoraje.infrastructure.util.IdentifierNormalizer;
import com.davivienda.factoraje.infrastructure.util.PageRequests;
import com.davivienda.factoraje.repository.BankAccountRepository;
import com.davivienda.factoraje.repository.CreditFacilityHistoryRepository;
import com.davivienda.factoraje.repository.CreditFacilityRepository;
import com.davivienda.factoraje.repository.EntityRepository;
import com.davivienda.factoraje.repository.EntityTypeCatRepository;
import com.davivienda.factoraje.repository.ProductPricingTermRepository;
import com.davivienda.factoraje.repository.RoleCatRepository;
import com.davivienda.factoraje.service.EntityService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EntityServiceImpl implements EntityService {

    private final EntityRepository entityRepository;
    private final EntityTypeCatRepository entityTypeCatRepository;
    private final CreditFacilityRepository creditFacilityRepository;
    private final ProductPricingTermRepository productPricingTermRepository;
    private final CreditFacilityHistoryRepository creditFacilityHistoryRepository;
    private final BankAccountRepository bankAccountRepository;
    private final RoleCatRepository roleCatRepository;
    private final CurrentUserService currentUserService;
    private final MailNoticePublisher mailNotices;

    private void validateUniqueness(EntityDTORequest entity) {
        if (entityRepository.existsByNit(entity.nit())) {
            log.warn("Ya existe una entidad registrada con el NIT indicado.");
            throw new ResourceAlreadyExistsException(
                    "Ya existe una entidad registrada con el NIT indicado.");
        }

        if (entityRepository.existsByName(entity.name())) {
            log.warn("Operación rechazada: Ya existe una entidad con el nombre proporcionado");
            throw new ResourceAlreadyExistsException(
                    "Ya existe una entidad registrada con el nombre indicado.");
        }
    }

    private String generateMnemonicCode(String name) {

        if (name == null || name.trim().isEmpty()) {
            return "GEN-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        }

        String cleanString = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("[^a-zA-Z0-9 ]", "")
                .replaceAll("\\s+", "")
                .toUpperCase();

        if (cleanString.isEmpty()) {
            return "ENT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        }

        // Hasta 6 letras del nombre y 4 caracteres aleatorios. Ej: "Cooperativa" -> "COOPER" + "A1B2".
        String prefix = cleanString.substring(0, Math.min(cleanString.length(), 6));
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + uniqueSuffix;
    }

    @Override
    @Transactional
    public EntityDTOResponse createEntity(EntityDTORequest rawRequest) {

        log.info("Starting the creation of a new entity");

        EntityDTORequest request = new EntityDTORequest(
                IdentifierNormalizer.requireNit(rawRequest.nit()),
                rawRequest.name() != null ? rawRequest.name().trim() : null,
                rawRequest.entityTypeId());

        validateUniqueness(request);

        log.info("Searching for entity type with identifier: {}", request.entityTypeId());

        EntityTypeCat entityType = entityTypeCatRepository.findById(request.entityTypeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el tipo de entidad con ID: " + request.entityTypeId()));

        String mnemonicCode = generateMnemonicCode(request.name());
        log.info("Generated mnemonic code: {}", mnemonicCode);

        EntityModel entityModel = EntityModel.builder()
                .nit(request.nit())
                .name(request.name())
                .entityType(entityType)
                .code(mnemonicCode)
                .status(GeneralStatusEnum.ACTIVE)
                .build();

        EntityModel savedEntity = entityRepository.save(entityModel);
        log.info("Entity created successfully with ID: {}", savedEntity.getId());
        return EntityDTOResponse.fromEntity(savedEntity);

    }

    @Override
    @Transactional
    public EntityModel createSupplier(String rawNit, String rawName) {
        String nit = IdentifierNormalizer.requireNit(rawNit);
        String name = rawName != null ? rawName.trim() : null;

        if (entityRepository.existsByNit(nit)) {
            throw new ResourceAlreadyExistsException("Ya existe una entidad registrada con el NIT " + nit + ".");
        }
        if (entityRepository.existsByName(name)) {
            throw new ResourceAlreadyExistsException("Ya existe una entidad registrada con el nombre " + name + ".");
        }

        EntityTypeCat supplierType = entityTypeCatRepository.findByCode(EntityTypeCode.SUPPLIER)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el tipo de entidad proveedor (" + EntityTypeCode.SUPPLIER + ")."));

        EntityModel supplier = entityRepository.save(EntityModel.builder()
                .nit(nit)
                .name(name)
                .code(generateMnemonicCode(name))
                .entityType(supplierType)
                .status(GeneralStatusEnum.ACTIVE)
                .build());
        log.info("Supplier created from document upload with ID: {}", supplier.getId());
        return supplier;
    }

    @Override
    @Transactional
    public PayerSummaryDTOResponse registerPayer(PayerRegistrationDTORequest request) {

        String nit = IdentifierNormalizer.requireNit(request.nit());
        String name = request.name().trim();
        String accountNumber = IdentifierNormalizer.requireAccountNumber(request.accountNumber());
        String creditFacilityNumber = request.creditFacilityNumber().trim();

        log.info("Registering payer with NIT {}", nit);

        if (entityRepository.existsByNit(nit)) {
            throw new ResourceAlreadyExistsException("Ya existe una entidad con el NIT " + nit + ".");
        }
        if (entityRepository.existsByName(name)) {
            throw new ResourceAlreadyExistsException("Ya existe una entidad con el nombre " + name + ".");
        }
        if (bankAccountRepository.existsByAccountNumber(accountNumber)) {
            throw new ResourceAlreadyExistsException(
                    "La cuenta bancaria " + accountNumber + " ya está registrada para otra entidad.");
        }
        if (creditFacilityRepository.existsByCreditFacilityNumber(creditFacilityNumber)) {
            throw new ResourceAlreadyExistsException(
                    "Ya existe un cupo de crédito con el número " + creditFacilityNumber + ".");
        }

        BigDecimal initialAmountInUse = request.initialAmountInUse() != null
                ? request.initialAmountInUse()
                : BigDecimal.ZERO;
        String initialConsumptionReference = request.initialConsumptionReference() != null
                ? request.initialConsumptionReference().trim()
                : "";
        boolean hasInitialConsumption = initialAmountInUse.signum() > 0;

        if (initialAmountInUse.compareTo(request.facilityLimitAmount()) > 0) {
            throw new IllegalArgumentException(
                    "El monto consumido no puede superar el monto aprobado del cupo.");
        }
        if (hasInitialConsumption && initialConsumptionReference.isEmpty()) {
            throw new IllegalArgumentException(
                    "La referencia del consumo es obligatoria cuando el cupo tiene un monto consumido.");
        }

        EntityTypeCat payerType = entityTypeCatRepository.findByCode(EntityTypeCode.PAYER)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el tipo de entidad pagador (" + EntityTypeCode.PAYER + ")."));

        UserModel executedBy = hasInitialConsumption ? currentUserService.managed() : null;

        try {
            EntityModel payer = entityRepository.saveAndFlush(EntityModel.builder()
                    .nit(nit)
                    .name(name)
                    .code(generateMnemonicCode(name))
                    .entityType(payerType)
                    .status(GeneralStatusEnum.ACTIVE)
                    .build());

            BankAccountModel account = bankAccountRepository.saveAndFlush(BankAccountModel.builder()
                    .accountNumber(accountNumber)
                    .entityModel(payer)
                    .isMain(true)
                    .status(GeneralStatusEnum.ACTIVE)
                    .build());

            CreditFacilityModel facility = creditFacilityRepository.saveAndFlush(CreditFacilityModel.builder()
                    .creditFacilityNumber(creditFacilityNumber)
                    .facilityLimitAmount(request.facilityLimitAmount())
                    .amountInUse(initialAmountInUse)
                    .warningThresholdPercentage(request.warningThresholdPercentage())
                    .status(GeneralStatusEnum.ACTIVE)
                    .payer(payer)
                    .build());

            if (hasInitialConsumption) {
                creditFacilityHistoryRepository.save(CreditFacilityHistoryModel.builder()
                        .amount(initialAmountInUse)
                        .referenceNumber(initialConsumptionReference)
                        .repaymentType(RepaymentTypeEnum.INITIAL_BALANCE)
                        .creditFacility(facility)
                        .payer(payer)
                        .executedBy(executedBy)
                        .build());
            }

            ProductPricingTermModel pricing = productPricingTermRepository.saveAndFlush(ProductPricingTermModel.builder()
                    .interestRate(request.interestRate())
                    .commissionRate(request.commissionRate())
                    .calculationBase(request.calculationBase())
                    .status(GeneralStatusEnum.ACTIVE)
                    .creditFacility(facility)
                    .build());

            log.info("Payer {} registered with credit facility {} and pricing term {}",
                    payer.getId(), facility.getId(), pricing.getId());

            mailNotices.operatorChanged("Pagadores", MailNoticePublisher.NO_RECORD,
                    describePayer(payer, account, facility, pricing));
            return PayerSummaryDTOResponse.from(payer, account, facility, pricing);

        } catch (DataIntegrityViolationException e) {
            log.error("Integrity violation when registering payer with NIT {}", nit, e);
            throw new ResourceAlreadyExistsException(
                    "El NIT, el código o la cuenta bancaria del pagador ya está registrado.");
        }
    }

    @Override
    @Transactional
    public PayerSummaryDTOResponse updatePayer(UUID payerId, PayerUpdateDTORequest request) {

        log.info("Updating payer {}", payerId);

        EntityModel payer = entityRepository.findById(payerId)
                .filter(entity -> entity.getEntityType() != null
                        && EntityTypeCode.PAYER.equals(entity.getEntityType().getCode()))
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el pagador especificado."));

        // Hibernate actualiza todas las columnas: sin el bloqueo, amount_in_use volvería al valor leído.
        CreditFacilityModel facility = creditFacilityRepository.findByPayerIdForUpdate(payerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El pagador no tiene un cupo de crédito asignado; no se pueden actualizar sus condiciones."));

        ProductPricingTermModel pricing = productPricingTermRepository.findByCreditFacilityId(facility.getId())
                .orElseGet(() -> ProductPricingTermModel.builder().creditFacility(facility).build());

        BankAccountModel account = bankAccountRepository.findFirstByEntityModelIdAndIsMainTrue(payerId).orElse(null);
        String previous = describePayer(payer, account, facility, pricing);
        String accountNumber = null;
        if (account != null) {
            accountNumber = IdentifierNormalizer.requireAccountNumber(request.accountNumber());
            if (bankAccountRepository.existsByAccountNumberAndIdNot(accountNumber, account.getId())) {
                throw new ResourceAlreadyExistsException(
                        "La cuenta bancaria " + accountNumber + " ya está registrada para otra entidad.");
            }
            account.setAccountNumber(accountNumber);
        } else if (request.accountNumber() != null && !request.accountNumber().isBlank()) {
            throw new IllegalArgumentException(
                    "El pagador no tiene una cuenta bancaria principal registrada; no se puede modificar.");
        }

        payer.setStatus(request.status());

        facility.setWarningThresholdPercentage(request.warningThresholdPercentage());
        facility.setStatus(request.status());

        pricing.setInterestRate(request.interestRate());
        pricing.setCommissionRate(request.commissionRate());
        pricing.setCalculationBase(request.calculationBase());
        pricing.setStatus(request.status());

        try {
            EntityModel savedPayer = entityRepository.saveAndFlush(payer);
            BankAccountModel savedAccount = account != null ? bankAccountRepository.saveAndFlush(account) : null;
            CreditFacilityModel savedFacility = creditFacilityRepository.saveAndFlush(facility);
            ProductPricingTermModel savedPricing = productPricingTermRepository.saveAndFlush(pricing);

            log.info("Payer {} updated", payerId);

            mailNotices.operatorChanged("Pagadores", previous, describePayer(payer, account, facility, pricing));
            return PayerSummaryDTOResponse.from(savedPayer, savedAccount, savedFacility, savedPricing);
        } catch (DataIntegrityViolationException e) {
            log.error("Integrity violation when updating payer {}", payerId, e);
            throw new ResourceAlreadyExistsException(
                    "La cuenta bancaria " + accountNumber + " ya está registrada para otra entidad.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierSummaryDTOResponse> getSupplierSummaries(String search, int page, int size) {

        Page<EntityModel> suppliers = findEntitiesPage(EntityTypeCode.SUPPLIER, search, page, size);
        if (!suppliers.hasContent()) {
            return suppliers.map(supplier -> SupplierSummaryDTOResponse.from(supplier, null));
        }

        Map<UUID, BankAccountModel> accountBySupplier = bankAccountRepository
                .findAllByEntityModelIdInAndIsMainTrue(suppliers.map(EntityModel::getId).getContent())
                .stream()
                .collect(Collectors.toMap(a -> a.getEntityModel().getId(), Function.identity(), (a, b) -> a));

        return suppliers.map(supplier -> SupplierSummaryDTOResponse.from(supplier, accountBySupplier.get(supplier.getId())));
    }

    private static String describePayer(EntityModel payer, BankAccountModel account,
            CreditFacilityModel facility, ProductPricingTermModel pricing) {
        StringBuilder text = new StringBuilder("Pagador ").append(AuditText.value(payer.getName()))
                .append(", estado ").append(AuditText.status(payer.getStatus()));
        if (account != null) {
            text.append(", cuenta ").append(AuditText.value(account.getAccountNumber()));
        }
        if (facility != null) {
            text.append(", cupo ").append(AuditText.amount(facility.getFacilityLimitAmount()))
                    .append(", umbral de alerta ").append(AuditText.value(facility.getWarningThresholdPercentage())).append('%');
        }
        if (pricing != null) {
            text.append(", tasa de interés ").append(AuditText.value(pricing.getInterestRate()))
                    .append(", comisión ").append(AuditText.value(pricing.getCommissionRate()))
                    .append(", base de cálculo ").append(AuditText.value(pricing.getCalculationBase()));
        }
        return text.toString();
    }

    private static String describeSupplier(EntityModel supplier, BankAccountModel account) {
        return "Proveedor " + AuditText.value(supplier.getName())
                + ", estado " + AuditText.status(supplier.getStatus())
                + ", cuenta " + AuditText.value(account.getAccountNumber());
    }

    /** Página de entidades del tipo ordenada por nombre; la búsqueda compara nombre y NIT. */
    private Page<EntityModel> findEntitiesPage(String typeCode, String search, int page, int size) {
        Pageable pageable = PageRequests.of(page, size, Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id")));

        if (search == null || search.isBlank()) {
            return entityRepository.findAllByEntityTypeCode(typeCode, pageable);
        }

        String escaped = search.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return entityRepository.searchByEntityTypeCode(typeCode, "%" + escaped + "%", pageable);
    }

    @Override
    @Transactional
    public SupplierSummaryDTOResponse updateSupplier(UUID supplierId, SupplierUpdateDTORequest request) {

        log.info("Updating supplier {}", supplierId);

        EntityModel supplier = entityRepository.findById(supplierId)
                .filter(entity -> entity.getEntityType() != null
                        && EntityTypeCode.SUPPLIER.equals(entity.getEntityType().getCode()))
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el proveedor especificado."));

        String accountNumber = IdentifierNormalizer.requireAccountNumber(request.accountNumber());

        BankAccountModel account = bankAccountRepository.findFirstByEntityModelIdAndIsMainTrue(supplierId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El proveedor no tiene una cuenta bancaria principal registrada."));

        if (bankAccountRepository.existsByAccountNumberAndIdNot(accountNumber, account.getId())) {
            throw new ResourceAlreadyExistsException(
                    "La cuenta bancaria " + accountNumber + " ya está registrada para otra entidad.");
        }

        String previous = describeSupplier(supplier, account);
        account.setAccountNumber(accountNumber);
        supplier.setStatus(request.status());

        try {
            EntityModel savedSupplier = entityRepository.saveAndFlush(supplier);
            BankAccountModel savedAccount = bankAccountRepository.saveAndFlush(account);

            log.info("Supplier {} updated", supplierId);

            mailNotices.operatorChanged("Proveedores", previous, describeSupplier(supplier, account));
            return SupplierSummaryDTOResponse.from(savedSupplier, savedAccount);
        } catch (DataIntegrityViolationException e) {
            log.error("Integrity violation when updating supplier {}", supplierId, e);
            throw new ResourceAlreadyExistsException(
                    "La cuenta bancaria " + accountNumber + " ya está registrada para otra entidad.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayerSummaryDTOResponse> getPayerSummaries(String search, int page, int size) {

        Page<EntityModel> payers = findEntitiesPage(EntityTypeCode.PAYER, search, page, size);
        if (!payers.hasContent()) {
            return payers.map(payer -> PayerSummaryDTOResponse.from(payer, null, null, null));
        }

        List<UUID> payerIds = payers.map(EntityModel::getId).getContent();

        Map<UUID, BankAccountModel> accountByPayer = bankAccountRepository
                .findAllByEntityModelIdInAndIsMainTrue(payerIds)
                .stream()
                .collect(Collectors.toMap(a -> a.getEntityModel().getId(), Function.identity(), (a, b) -> a));

        Map<UUID, CreditFacilityModel> facilityByPayer = creditFacilityRepository
                .findAllByPayerIdIn(payerIds)
                .stream()
                .collect(Collectors.toMap(f -> f.getPayer().getId(), Function.identity(), (a, b) -> a));

        Map<UUID, ProductPricingTermModel> pricingByFacility = facilityByPayer.isEmpty()
                ? Map.of()
                : productPricingTermRepository
                        .findAllByCreditFacilityIdIn(facilityByPayer.values().stream()
                                .map(CreditFacilityModel::getId).toList())
                        .stream()
                        .collect(Collectors.toMap(p -> p.getCreditFacility().getId(), Function.identity(),
                                (a, b) -> a));

        return payers.map(payer -> {
            CreditFacilityModel facility = facilityByPayer.get(payer.getId());
            ProductPricingTermModel pricing = facility != null ? pricingByFacility.get(facility.getId()) : null;
            return PayerSummaryDTOResponse.from(payer, accountByPayer.get(payer.getId()), facility, pricing);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EntityDTOResponse> getPayers(Pageable pageable) {
        log.info("Obteniendo catálogo de entidades de tipo Pagador");
        return entityRepository.findAllByEntityTypeCode(EntityTypeCode.PAYER, pageable)
                .map(EntityDTOResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EntityDTOResponse> getSuppliers(Pageable pageable) {
        log.info("Obteniendo catálogo de entidades de tipo Proveedor");
        return entityRepository.findAllByEntityTypeCode(EntityTypeCode.SUPPLIER, pageable)
                .map(EntityDTOResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EntityDTOResponse> getEntities(Pageable pageable) {
        log.info("Obteniendo catálogo de entidades");
        return entityRepository.findAll(pageable)
                .map(EntityDTOResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EntityDTOResponse> searchEntitiesForRole(UUID roleId, String search, int page, int size) {
        RoleCat role = roleCatRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el rol con ID: " + roleId));
        String typeCode = EntityTypeCode.forRole(role.getName());
        if (typeCode == null) {
            throw new IllegalArgumentException("El rol " + role.getName() + " no tiene un tipo de entidad asociado.");
        }
        return findEntitiesPage(typeCode, search, page, size).map(EntityDTOResponse::fromEntity);
    }

}
